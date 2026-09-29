package com.example.ui.components.shelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Game
import com.example.ui.components.Interactive3DGameCard

/**
 * Steam-style Vertical Poster Grid View featuring dense, immersive poster covers
 * with realistic 3D physical game case rotation and physical depth.
 *
 * Game information (title, console, year, TTB) remains OUTSIDE the 3D case.
 */
@Composable
fun SteamGridView(
    games: List<Game>,
    onSelectGame: (Game) -> Unit,
    onTrailerClick: (Game) -> Unit,
    onDeleteClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("shelf_steam_grid"),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 10.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(games, key = { it.id }) { game ->
            SteamGameCard(
                game = game,
                onClick = { onSelectGame(game) },
                onTrailerClick = { onTrailerClick(game) },
                onDeleteClick = { onDeleteClick(game) }
            )
        }
    }
}

@Composable
fun SteamGameCard(
    game: Game,
    onClick: () -> Unit,
    onTrailerClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val console = game.console

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("steam_card_${game.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Realistic 3D Physical Game Case
        Interactive3DGameCard(
            game = game,
            aspectRatio = 2f / 3f,
            hasPhysicalSpine = true,
            enable3DRotation = false,
            onClick = onClick,
            onTrailerClick = onTrailerClick,
            onDeleteClick = onDeleteClick,
            showDeleteButton = true,
            showTrailerButton = true,
            showDetailsButton = false
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Game metadata outside 3D case
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = console.shortName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(console.accentColorHex),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (game.hltbMainStoryHours > 0f) {
                    Text(
                        text = "⏱${game.formattedMainStory}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF7DD3FC),
                            fontSize = 8.5.sp
                        )
                    )
                } else if (game.releaseYear > 0) {
                    Text(
                        text = "${game.releaseYear}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp
                        )
                    )
                }
            }
        }
    }
}
