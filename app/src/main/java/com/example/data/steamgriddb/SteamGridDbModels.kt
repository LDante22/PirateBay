package com.example.data.steamgriddb

import androidx.compose.runtime.Immutable

@Immutable
data class SteamGridDbGame(
    val id: Long,
    val name: String,
    val releaseDate: Long = 0L,
    val types: List<String> = emptyList()
)

@Immutable
data class SteamGridDbGrid(
    val id: Long,
    val score: Int,
    val style: String = "",
    val width: Int = 600,
    val height: Int = 900,
    val nsfw: Boolean = false,
    val humor: Boolean = false,
    val url: String,
    val thumb: String,
    val author: String = ""
)

sealed class SteamGridDbResult<out T> {
    data class Success<T>(val data: T) : SteamGridDbResult<T>()
    data class AmbiguousMatches(val games: List<SteamGridDbGame>) : SteamGridDbResult<Nothing>()
    data class NotFound(val message: String = "Nu s-a găsit nicio copertă pentru acest joc.") : SteamGridDbResult<Nothing>()
    data class Error(
        val statusCode: Int,
        val message: String,
        val canRetry: Boolean = true
    ) : SteamGridDbResult<Nothing>()
}

data class SteamGridDbResolvedCover(
    val gameId: Long,
    val gameTitle: String,
    val fullCoverUrl: String,
    val thumbCoverUrl: String,
    val score: Int,
    val availableGrids: List<SteamGridDbGrid> = emptyList()
)
