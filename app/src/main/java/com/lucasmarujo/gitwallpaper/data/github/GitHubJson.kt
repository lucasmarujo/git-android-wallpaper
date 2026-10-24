package com.lucasmarujo.gitwallpaper.data.github

import kotlinx.serialization.json.Json

val GitHubJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
