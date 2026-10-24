package com.lucasmarujo.gitwallpaper.domain

data class UpdateStatus(
    val fingerprint: String? = null,
    val lastAppliedAtMillis: Long? = null,
    val lastCheckedAtMillis: Long? = null,
)
