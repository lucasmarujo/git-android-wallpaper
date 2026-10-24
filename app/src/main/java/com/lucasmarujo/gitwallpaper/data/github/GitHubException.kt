package com.lucasmarujo.gitwallpaper.data.github

import java.io.IOException

sealed class GitHubException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Unauthorized : GitHubException("GitHub rejected the access token")

    class Network(cause: IOException) : GitHubException("Network failure while calling GitHub", cause)

    class Http(val code: Int) : GitHubException("GitHub responded with HTTP $code")

    class Api(message: String) : GitHubException(message)
}
