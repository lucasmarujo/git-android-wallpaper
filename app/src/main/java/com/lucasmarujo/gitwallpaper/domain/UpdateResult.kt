package com.lucasmarujo.gitwallpaper.domain

sealed interface UpdateResult {
    data object Applied : UpdateResult

    data object Unchanged : UpdateResult

    data class Failure(val error: SyncError) : UpdateResult
}
