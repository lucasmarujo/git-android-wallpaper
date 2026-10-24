package com.lucasmarujo.gitwallpaper.domain

enum class UpdateInterval(val minutes: Long) {
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    THREE_HOURS(180),
    SIX_HOURS(360),
    TWELVE_HOURS(720),
}
