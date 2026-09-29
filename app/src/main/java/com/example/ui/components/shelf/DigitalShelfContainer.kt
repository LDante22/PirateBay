package com.example.ui.components.shelf

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Game
import com.example.data.model.ShelfViewMode

@Composable
fun DigitalShelfContainer(
    games: List<Game>,
    currentMode: ShelfViewMode,
    onSelectGame: (Game) -> Unit,
    onTrailerClick: (Game) -> Unit,
    onDeleteClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("digital_shelf_container")
    ) {
        Crossfade(
            targetState = currentMode,
            animationSpec = tween(220),
            label = "shelf_mode_crossfade"
        ) { mode ->
            when (mode) {
                ShelfViewMode.CLASSIC_GRID -> {
                    ClassicGridView(
                        games = games,
                        onSelectGame = onSelectGame,
                        onTrailerClick = onTrailerClick,
                        onDeleteClick = onDeleteClick,
                        contentPadding = contentPadding
                    )
                }
                ShelfViewMode.STEAM_GRID -> {
                    SteamGridView(
                        games = games,
                        onSelectGame = onSelectGame,
                        onTrailerClick = onTrailerClick,
                        onDeleteClick = onDeleteClick,
                        contentPadding = contentPadding
                    )
                }
                ShelfViewMode.RETRO_SHELF -> {
                    RetroShelfView(
                        games = games,
                        onSelectGame = onSelectGame,
                        onTrailerClick = onTrailerClick,
                        onDeleteClick = onDeleteClick,
                        contentPadding = contentPadding
                    )
                }
                ShelfViewMode.BOX_ART_3D -> {
                    BoxArt3DView(
                        games = games,
                        onSelectGame = onSelectGame,
                        onTrailerClick = onTrailerClick,
                        onDeleteClick = onDeleteClick,
                        contentPadding = contentPadding
                    )
                }
                ShelfViewMode.CAROUSEL -> {
                    CarouselShelfView(
                        games = games,
                        onSelectGame = onSelectGame,
                        onTrailerClick = onTrailerClick,
                        onDeleteClick = onDeleteClick,
                        contentPadding = contentPadding
                    )
                }
            }
        }
    }
}
