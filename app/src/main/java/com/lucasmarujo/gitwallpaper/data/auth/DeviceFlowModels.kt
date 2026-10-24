package com.lucasmarujo.gitwallpaper.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String? = null,
    @SerialName("user_code") val userCode: String? = null,
    @SerialName("verification_uri") val verificationUri: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    val interval: Int? = null,
    val error: String? = null,
)

@Serializable
internal data class AccessTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    val interval: Int? = null,
    val error: String? = null,
)
