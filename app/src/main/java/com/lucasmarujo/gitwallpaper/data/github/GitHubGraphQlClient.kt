package com.lucasmarujo.gitwallpaper.data.github

import com.lucasmarujo.gitwallpaper.domain.ContributionCalendar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GitHubGraphQlClient(
    private val httpClient: OkHttpClient,
    private val endpoint: HttpUrl = DEFAULT_ENDPOINT.toHttpUrl(),
    private val json: Json = GitHubJson,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ContributionsSource {

    override suspend fun fetchCalendar(token: String): ContributionCalendar {
        val requestBody = json.encodeToString(GraphQlRequest.serializer(), GraphQlRequest(CONTRIBUTIONS_QUERY))
        val request = Request.Builder()
            .url(endpoint)
            .header(AUTHORIZATION_HEADER, "Bearer $token")
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()
        val response = json.decodeFromString(
            GraphQlResponse.serializer(ViewerData.serializer()),
            execute(request),
        )
        response.errors?.firstOrNull()?.let { throw GitHubException.Api(it.message) }
        val viewer = response.data?.viewer ?: throw GitHubException.Api(MISSING_VIEWER_MESSAGE)
        return viewer.toCalendar()
    }

    private suspend fun execute(request: Request): String = withContext(ioDispatcher) {
        try {
            httpClient.newCall(request).execute().use { response ->
                when {
                    response.code == HTTP_UNAUTHORIZED -> throw GitHubException.Unauthorized()
                    !response.isSuccessful -> throw GitHubException.Http(response.code)
                    else -> response.body.string()
                }
            }
        } catch (exception: IOException) {
            throw GitHubException.Network(exception)
        }
    }

    private companion object {
        const val DEFAULT_ENDPOINT = "https://api.github.com/graphql"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        const val HTTP_UNAUTHORIZED = 401
        const val MISSING_VIEWER_MESSAGE = "GitHub returned no viewer data"
        const val CONTRIBUTIONS_QUERY = """
            query {
              viewer {
                login
                contributionsCollection {
                  contributionCalendar {
                    totalContributions
                    weeks {
                      contributionDays {
                        date
                        contributionCount
                        contributionLevel
                      }
                    }
                  }
                }
              }
            }
        """
    }
}
