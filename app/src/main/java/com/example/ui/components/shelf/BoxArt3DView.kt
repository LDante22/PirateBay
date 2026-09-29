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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Game
import com.example.data.model.WishlistStatus
import com.example.ui.components.Interactive3DGameCard

/**
 * 3D Physical Box Art shelf view showcasing collectible physical game boxes.
 * Dragging horizontally across any game case rotates it in 3D perspective space,
 * revealing its physical depth, console-branded spine, clear opening edge, and realistic plastic sleeve sheen.
 *
 * All game information (title, console, TTB, emulator, status) remains OUTSIDE the 3D case.
 */
@Composable
fun BoxArt3DView(
    games: List<Game>,
    onSelectGame: (Game) -> Unit,
    onTrailerClick: (Game) -> Unit,
    onDeleteClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 135.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("shelf_box_art_3d_grid"),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(games, key = { it.id }) { game ->
            BoxArt3DItem(
                game = game,
                onClick = { onSelectGame(game) },
                onTrailerClick = { onTrailerClick(game) },
                onDeleteClick = { onDeleteClick(game) }
            )
        }
    }
}

@Composable
fun BoxArt3DItem(
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
            .testTag("3d_box_${game.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Physical Game Case Cover (Static in grid browsing)
        Interactive3DGameCard(
            game = game,
            aspectRatio = 0.74f,
            hasPhysicalSpine = true,
            enable3DRotation = false,
            onClick = onClick,
            onTrailerClick = onTrailerClick,
            onDeleteClick = onDeleteClick,
            showDeleteButton = true,
            showTrailerButton = true,
            showDetailsButton = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Title and Metadata Below Box (Tapping opens details sheet)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = Color(0xFFF1EEF7)
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = console.shortName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(console.accentColorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                )
                if (game.releaseYear > 0) {
                    Text(
                        text = "• ${game.releaseYear}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF938F99),
                            fontSize = 9.sp
                        )
                    )
                }
                if (game.hltbMainStoryHours > 0f) {
                    Text(
                        text = "• ⏱${game.formattedMainStory}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF80D4FF),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Optional emulator chip or wishlist status
            val status = WishlistStatus.fromId(game.wishlistStatus)
            val emu = game.emulator.ifBlank { console.primaryEmulator() }
            if (emu.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color(0xFF1E1B29),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = emu,
                            color = Color(0xFFCFBCFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = status.label,
                        color = Color(status.badgeColorHex),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
