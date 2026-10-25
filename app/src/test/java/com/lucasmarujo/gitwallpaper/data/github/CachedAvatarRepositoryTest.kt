package com.lucasmarujo.gitwallpaper.data.github

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CachedAvatarRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `missing urls return no avatar`() = runTest {
        assertNull(repository().load(null))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `urls outside the github avatar cdn are never requested`() = runTest {
        val repository = CachedAvatarRepository(OkHttpClient(), cacheDirectory(), ioDispatcher = Dispatchers.Unconfined)

        assertNull(repository.load(server.url("/avatar.png").toString()))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `downloads and decodes the avatar`() = runTest {
        server.enqueue(image(Color.RED))

        val avatar = repository().load(avatarUrl())

        assertEquals(IMAGE_SIZE, avatar?.width)
        assertEquals(Color.RED, avatar?.getPixel(1, 1))
    }

    @Test
    fun `cached avatars are reused across instances without downloading again`() = runTest {
        server.enqueue(image(Color.RED))
        repository().load(avatarUrl())

        val cached = repository().load(avatarUrl())

        assertEquals(Color.RED, cached?.getPixel(1, 1))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a new avatar url downloads the new image`() = runTest {
        server.enqueue(image(Color.RED))
        server.enqueue(image(Color.BLUE))
        val repository = repository()
        repository.load(avatarUrl("v=1"))

        val updated = repository.load(avatarUrl("v=2"))

        assertEquals(Color.BLUE, updated?.getPixel(1, 1))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `server errors return no avatar`() = runTest {
        server.enqueue(MockResponse(code = 404, body = ""))

        assertNull(repository().load(avatarUrl()))
    }

    @Test
    fun `invalid images are not cached`() = runTest {
        server.enqueue(MockResponse(code = 200, body = "not an image"))
        server.enqueue(image(Color.RED))
        val repository = repository()

        assertNull(repository.load(avatarUrl()))
        assertEquals(Color.RED, repository.load(avatarUrl())?.getPixel(1, 1))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `oversized downloads are rejected`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body(Buffer().write(ByteArray(OVERSIZED_BYTES))).build())

        assertNull(repository().load(avatarUrl()))
    }

    @Test
    fun `only https github avatar hosts are allowed`() {
        assertTrue(isGitHubAvatarUrl("https://avatars.githubusercontent.com/u/1?v=4".toHttpUrl()))
        assertFalse(isGitHubAvatarUrl("http://avatars.githubusercontent.com/u/1".toHttpUrl()))
        assertFalse(isGitHubAvatarUrl("https://evilgithubusercontent.com/u/1".toHttpUrl()))
        assertFalse(isGitHubAvatarUrl("https://avatars.githubusercontent.com.evil.com/u/1".toHttpUrl()))
    }

    private fun repository() = CachedAvatarRepository(
        httpClient = OkHttpClient(),
        cacheDirectory = cacheDirectory(),
        isAllowedUrl = { true },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun cacheDirectory() = File(temporaryFolder.root, "avatar")

    private fun avatarUrl(version: String = "v=4") = server.url("/u/1?$version").toString()

    private fun image(color: Int): MockResponse {
        val bitmap = createBitmap(IMAGE_SIZE, IMAGE_SIZE).apply { eraseColor(color) }
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        return MockResponse.Builder().code(200).body(Buffer().write(bytes)).build()
    }

    private companion object {
        const val IMAGE_SIZE = 8
        const val OVERSIZED_BYTES = 1_048_577
    }
}
