package com.lucasmarujo.gitwallpaper.data.github

import com.lucasmarujo.gitwallpaper.domain.ContributionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GitHubGraphQlClientTest {

    private val server = MockWebServer()
    private lateinit var client: GitHubGraphQlClient

    @Before
    fun setUp() {
        server.start()
        client = GitHubGraphQlClient(
            httpClient = OkHttpClient.Builder().retryOnConnectionFailure(false).build(),
            endpoint = server.url("/graphql"),
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `fetches the viewer calendar with a bearer token`() = runTest {
        server.enqueue(MockResponse(code = 200, body = CALENDAR_RESPONSE))

        val calendar = client.fetchCalendar("secret-token")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/graphql", request.url.encodedPath)
        assertEquals("Bearer secret-token", request.headers["Authorization"])
        assertTrue(request.body!!.utf8().contains("contributionCalendar"))
        assertEquals("octocat", calendar.login)
        assertEquals(7, calendar.totalContributions)
        assertEquals(3, calendar.days.size)
    }

    @Test
    fun `days are flattened across weeks and sorted by date`() = runTest {
        server.enqueue(MockResponse(code = 200, body = CALENDAR_RESPONSE))

        val calendar = client.fetchCalendar("token")

        assertEquals(
            listOf(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19)),
            calendar.days.map { it.date },
        )
        assertEquals(listOf(5, 0, 2), calendar.days.map { it.count })
        assertEquals(ContributionLevel.FOURTH_QUARTILE, calendar.days.first().level)
    }

    @Test
    fun `unknown contribution levels fall back to none`() = runTest {
        server.enqueue(MockResponse(code = 200, body = CALENDAR_RESPONSE))

        val calendar = client.fetchCalendar("token")

        assertEquals(ContributionLevel.NONE, calendar.days.last().level)
    }

    @Test(expected = GitHubException.Unauthorized::class)
    fun `http 401 is reported as unauthorized`() = runTest {
        server.enqueue(MockResponse(code = 401, body = """{"message":"Bad credentials"}"""))

        client.fetchCalendar("expired")
    }

    @Test
    fun `server errors are reported with their status code`() = runTest {
        server.enqueue(MockResponse(code = 502, body = ""))

        val exception = runCatching { client.fetchCalendar("token") }.exceptionOrNull()

        assertEquals(502, (exception as GitHubException.Http).code)
    }

    @Test
    fun `graphql errors are reported as api errors`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"data":null,"errors":[{"message":"Something broke"}]}"""))

        val exception = runCatching { client.fetchCalendar("token") }.exceptionOrNull()

        assertEquals("Something broke", (exception as GitHubException.Api).message)
    }

    @Test(expected = GitHubException.Network::class)
    fun `dropped connections are reported as network errors`() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        client.fetchCalendar("token")
    }

    private companion object {
        val CALENDAR_RESPONSE = """
            {
              "data": {
                "viewer": {
                  "login": "octocat",
                  "contributionsCollection": {
                    "contributionCalendar": {
                      "totalContributions": 7,
                      "weeks": [
                        {
                          "contributionDays": [
                            {"date": "2026-10-18", "contributionCount": 0, "contributionLevel": "NONE"},
                            {"date": "2026-10-19", "contributionCount": 2, "contributionLevel": "BRAND_NEW_LEVEL"}
                          ]
                        },
                        {
                          "contributionDays": [
                            {"date": "2026-10-17", "contributionCount": 5, "contributionLevel": "FOURTH_QUARTILE", "color": "#216e39"}
                          ]
                        }
                      ]
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }
}
