package com.example.ui.components.shelf

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Game
import com.example.data.model.WishlistStatus
import com.example.ui.components.Interactive3DGameCard

/**
 * Classic Grid View showcasing traditional box art posters with interactive 3D rotation/flip.
 */
@Composable
fun ClassicGridView(
    games: List<Game>,
    onSelectGame: (Game) -> Unit,
    onTrailerClick: (Game) -> Unit,
    onDeleteClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("shelf_classic_grid"),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(games, key = { it.id }) { game ->
            ClassicGameCard(
                game = game,
                onClick = { onSelectGame(game) },
                onTrailerClick = { onTrailerClick(game) },
                onDeleteClick = { onDeleteClick(game) }
            )
        }
    }
}

@Composable
fun ClassicGameCard(
    game: Game,
    onClick: () -> Unit,
    onTrailerClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val console = game.console
    val status = WishlistStatus.fromId(game.wishlistStatus)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("classic_card_${game.id}")
            .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color(console.accentColorHex).copy(alpha = 0.25f)),
        color = Color(0xFF16141F),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF282536))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Realistic Physical Game Case (Static in library grid)
            Interactive3DGameCard(
                game = game,
                aspectRatio = 2f / 3f,
                hasPhysicalSpine = true,
                enable3DRotation = false,
                onClick = onClick,
                onTrailerClick = onTrailerClick,
                onDeleteClick = onDeleteClick,
                showDeleteButton = false,
                showTrailerButton = true,
                showDetailsButton = false
            )

            // Subtle Metadata Below Artwork (Clickable to open sheet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF1F5F9),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subtle Status indicator
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(status.badgeColorHex),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Release Year or Rating
                    if (game.releaseYear > 0) {
                        Text(
                            text = "${game.releaseYear}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 9.5.sp
                            )
                        )
                    } else if (game.userRating > 0f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", game.userRating),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFBBF24),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
