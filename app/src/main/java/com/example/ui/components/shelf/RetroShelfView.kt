package com.example.ui.components.shelf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Game
import com.example.ui.components.Interactive3DGameCard
import kotlin.math.abs

@Composable
fun RetroShelfView(
    games: List<Game>,
    onSelectGame: (Game) -> Unit,
    onTrailerClick: (Game) -> Unit,
    onDeleteClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("retro_shelf_view")) {
        val screenWidth = maxWidth
        // Responsive calculation of games per shelf row
        val gamesPerShelf = when {
            screenWidth >= 1000.dp -> 7
            screenWidth >= 800.dp -> 5
            screenWidth >= 600.dp -> 4
            screenWidth >= 420.dp -> 3
            else -> 2
        }

        // Chunk games into rows/shelves
        val shelves = remember(games, gamesPerShelf) {
            games.chunked(gamesPerShelf)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            itemsIndexed(shelves, key = { index, _ -> "shelf_$index" }) { shelfIndex, shelfGames ->
                ShelfRow(
                    shelfNumber = shelfIndex + 1,
                    games = shelfGames,
                    expectedSlots = gamesPerShelf,
                    onSelectGame = onSelectGame,
                    onTrailerClick = onTrailerClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
private fun ShelfRow(
    shelfNumber: Int,
    games: List<Game>,
    expectedSlots: Int,
    onSelectGame: (Game) -> Unit,
    onTrailerClick: (Game) -> Unit,
    onDeleteClick: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Shelf Header Label (Subtle Brass Plate / Wood Carving)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF422006).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF92400E))
                ) {
                    Text(
                        text = "SHELF #$shelfNumber",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "${games.size} item${if (games.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp
                )
            )
        }

        // Row of physical standing game boxes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            games.forEachIndexed { index, game ->
                // Subtle rotation (+/- 1.5 deg) for realistic organic shelf feel
                val tiltAngle = remember(game.id) {
                    val hash = abs(game.id.hashCode() % 5)
                    when (hash) {
                        0 -> -1.2f
                        1 -> 1.0f
                        2 -> -0.8f
                        3 -> 1.4f
                        else -> 0.0f
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    StandingGameBox(
                        game = game,
                        tiltAngle = tiltAngle,
                        onClick = { onSelectGame(game) },
                        onTrailerClick = { onTrailerClick(game) },
                        onDeleteClick = { onDeleteClick(game) }
                    )
                }
            }

            // Fill empty slots on the last shelf to preserve grid width
            val emptySlots = expectedSlots - games.size
            if (emptySlots > 0) {
                repeat(emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // ==========================================
        // REALISTIC PHYSICAL WOODEN SHELF PLANK
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            // Top shelf surface bevel highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFB45309), // Amber-700
                                Color(0xFFD97706), // Amber-600
                                Color(0xFFF59E0B), // Amber-500
                                Color(0xFFD97706),
                                Color(0xFF78350F)
                            )
                        )
                    )
            )

            // Front mahogany wood plank body with horizontal grain
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .offset(y = 3.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF451A03), // Deep Walnut
                                Color(0xFF291102), // Dark Mahogany
                                Color(0xFF190902)
                            )
                        )
                    )
            ) {
                // Shelf edge wood grain lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color(0xFF78350F).copy(alpha = 0.4f),
                        start = Offset(0f, 4f),
                        end = Offset(size.width, 4f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = 0.5f),
                        start = Offset(0f, size.height - 1f),
                        end = Offset(size.width, size.height - 1f),
                        strokeWidth = 1.5f
                    )
                }
            }

            // Cast shadow below shelf
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun StandingGameBox(
    game: Game,
    tiltAngle: Float,
    onClick: () -> Unit,
    onTrailerClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val console = game.console

    Column(
        modifier = modifier
            .fillMaxWidth()
            .rotate(tiltAngle)
            .testTag("standing_box_${game.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Interactive3DGameCard(
            game = game,
            aspectRatio = 0.72f,
            hasPhysicalSpine = true,
            enable3DRotation = false,
            onClick = onClick,
            onTrailerClick = onTrailerClick,
            onDeleteClick = onDeleteClick,
            showDeleteButton = true,
            showTrailerButton = true,
            showDetailsButton = true
        )

        // Contact Ambient Shadow (where the box meets the shelf)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Title & Metadata Under Box (Clickable to open details sheet)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = game.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                color = Color(0xFFE2E8F0)
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .clickable { onClick() }
        )
        Row(
            modifier = Modifier.padding(top = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = console.shortName,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(console.accentColorHex),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp
                )
            )
            if (game.hltbMainStoryHours > 0f) {
                Text(
                    text = "• ⏱${game.formattedMainStory}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF7DD3FC),
                        fontSize = 8.sp
                    )
                )
            }
        }
    }
}
