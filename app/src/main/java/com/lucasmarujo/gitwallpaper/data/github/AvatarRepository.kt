package com.lucasmarujo.gitwallpaper.data.github

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

fun interface AvatarSource {
    suspend fun load(url: String?): Bitmap?
}

class CachedAvatarRepository(
    private val httpClient: OkHttpClient,
    private val cacheDirectory: File,
    private val isAllowedUrl: (HttpUrl) -> Boolean = ::isGitHubAvatarUrl,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AvatarSource {
    private val mutex = Mutex()
    private var memoryCache: Pair<String, Bitmap>? = null

    private val imageFile: File
        get() = File(cacheDirectory, IMAGE_FILE_NAME)

    private val urlFile: File
        get() = File(cacheDirectory, URL_FILE_NAME)

    override suspend fun load(url: String?): Bitmap? {
        val httpUrl = url?.toHttpUrlOrNull()?.takeIf(isAllowedUrl) ?: return null
        val key = httpUrl.toString()
        return mutex.withLock {
            memoryCache?.takeIf { it.first == key }?.second
                ?: withContext(ioDispatcher) { readCache(key) ?: download(httpUrl) }
                    ?.also { memoryCache = key to it }
        }
    }

    private fun readCache(key: String): Bitmap? {
        val cachedKey = try {
            urlFile.takeIf { it.exists() }?.readText()
        } catch (exception: IOException) {
            Log.w(TAG, "Could not read the avatar cache", exception)
            null
        }
        if (cachedKey != key) return null
        return BitmapFactory.decodeFile(imageFile.path)
    }

    private fun download(url: HttpUrl): Bitmap? {
        return try {
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val source = response.body.source()
                if (source.request(MAX_AVATAR_BYTES + 1)) return null
                val bytes = source.buffer.readByteArray()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                cacheDirectory.mkdirs()
                imageFile.writeBytes(bytes)
                urlFile.writeText(url.toString())
                bitmap
            }
        } catch (exception: IOException) {
            Log.w(TAG, "Could not download the GitHub avatar", exception)
            null
        }
    }

    private companion object {
        const val TAG = "AvatarRepository"
        const val IMAGE_FILE_NAME = "avatar.img"
        const val URL_FILE_NAME = "avatar.url"
        const val MAX_AVATAR_BYTES = 1_048_576L
    }
}

private const val GITHUB_AVATAR_HOST = "githubusercontent.com"

fun isGitHubAvatarUrl(url: HttpUrl): Boolean =
    url.isHttps && (url.host == GITHUB_AVATAR_HOST || url.host.endsWith(".$GITHUB_AVATAR_HOST"))
