package com.example.ui.components.shelf

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.example.data.model.WishlistStatus
import com.example.ui.components.Interactive3DGameCard
import kotlinx.coroutines.launch

@Composable
fun CarouselShelfView(
    games: List<Game>,
    onSelectGame: (Game) -> Unit,
    onTrailerClick: (Game) -> Unit,
    onDeleteClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (games.isEmpty()) return

    var currentIndex by remember(games) { mutableIntStateOf(0) }
    // Ensure valid index bounds
    if (currentIndex >= games.size) {
        currentIndex = games.size - 1
    }
    if (currentIndex < 0) {
        currentIndex = 0
    }

    val selectedGame = games[currentIndex]
    val console = selectedGame.console
    val status = WishlistStatus.fromId(selectedGame.wishlistStatus)
    val ribbonState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smoothly scroll ribbon to center the selected game
    LaunchedEffect(currentIndex) {
        if (currentIndex in games.indices) {
            ribbonState.animateScrollToItem(
                index = (currentIndex - 2).coerceAtLeast(0)
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .testTag("carousel_shelf_view")
    ) {
        val screenWidth = maxWidth
        val coverWidth = when {
            screenWidth >= 800.dp -> 180.dp
            screenWidth >= 600.dp -> 150.dp
            screenWidth < 360.dp -> 110.dp
            else -> 130.dp
        }
        val heroBannerHeight = if (screenWidth >= 600.dp) 280.dp else 220.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            // ==========================================
            // HERO SPOTLIGHT DISPLAY
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1A29),
                border = BorderStroke(1.dp, Color(0xFF3E3652))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Dynamic ambient background glow from game's banner or theme colors
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroBannerHeight)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(console.brandColorHex).copy(alpha = 0.35f),
                                        Color(selectedGame.coverGradientStart).copy(alpha = 0.25f),
                                        Color(0xFF1E1A29)
                                    )
                                )
                            )
                    )

                    if (selectedGame.bannerArtUrl.isNotBlank()) {
                        ShelfAsyncImage(
                            imageUrl = selectedGame.bannerArtUrl,
                            contentDescription = "${selectedGame.title} Banner",
                            contentScale = ContentScale.Crop,
                            crossfadeDurationMs = 250,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(heroBannerHeight)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        )
                        // Gradient overlay to darken banner for text readability
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(heroBannerHeight)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color(0xFF1E1A29).copy(alpha = 0.85f),
                                            Color(0xFF1E1A29)
                                        )
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Carousel Navigation Header [ < Previous ] [ Index / Total ] [ Next > ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        currentIndex--
                                    } else {
                                        currentIndex = games.size - 1
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B2538))
                                    .border(1.dp, Color(0xFF4F4666), CircleShape)
                                    .testTag("btn_carousel_prev")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Game",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Surface(
                                color = Color(0xFF2B2538),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, Color(0xFF4F4666))
                            ) {
                                Text(
                                    text = "${currentIndex + 1} / ${games.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFEADDFF),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentIndex < games.size - 1) {
                                        currentIndex++
                                    } else {
                                        currentIndex = 0
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B2538))
                                    .border(1.dp, Color(0xFF4F4666), CircleShape)
                                    .testTag("btn_carousel_next")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Game",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hero Content Row: [ Large Featured Box Artwork ] + [ Quick Details & CTAs ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Large Featured Static Game Case in Carousel
                            Interactive3DGameCard(
                                game = selectedGame,
                                aspectRatio = 0.72f,
                                hasPhysicalSpine = true,
                                enable3DRotation = false,
                                onClick = { onSelectGame(selectedGame) },
                                onTrailerClick = { onTrailerClick(selectedGame) },
                                onDeleteClick = { onDeleteClick(selectedGame) },
                                showDeleteButton = false,
                                showTrailerButton = false,
                                showDetailsButton = true,
                                modifier = Modifier.width(coverWidth)
                            )

                        // Right Column: Title, Console, Badges, Playtime & Synopsis
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = selectedGame.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color(0xFFF8FAFC)
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Badges Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(status.badgeColorHex).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color(status.badgeColorHex))
                                ) {
                                    Text(
                                        text = "${status.iconEmoji} ${status.label}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(status.badgeColorHex),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }

                                if (selectedGame.metacriticScore > 0) {
                                    Surface(
                                        color = when {
                                            selectedGame.metacriticScore >= 75 -> Color(0xFF15803D)
                                            selectedGame.metacriticScore >= 50 -> Color(0xFFA16207)
                                            else -> Color(0xFFB91C1C)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Metacritic ${selectedGame.metacriticScore}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Specs & Genre
                            Text(
                                text = "${console.displayName} • ${selectedGame.genre}${if (selectedGame.releaseYear > 0) " • ${selectedGame.releaseYear}" else ""}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            )

                            // HLTB Main Story playtime
                            if (selectedGame.hltbMainStoryHours > 0f) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Main Story: ${selectedGame.hltbMainStoryHours.toInt()}h",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            // Description snippet
                            if (selectedGame.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = selectedGame.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.5.sp,
                                        lineHeight = 14.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSelectGame(selectedGame) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_carousel_view_details"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (selectedGame.youtubeVideoId.isNotBlank()) {
                            OutlinedButton(
                                onClick = { onTrailerClick(selectedGame) },
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("btn_carousel_trailer"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFEF4444)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Trailer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteClick(selectedGame) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF361821))
                                .border(1.dp, Color(0xFF8C1D38), RoundedCornerShape(8.dp))
                                .testTag("btn_carousel_delete")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFFB4AB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // MINI THUMBNAIL RIBBON (Rapid Carousel Navigation)
        // ==========================================
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Text(
                text = "COLLECTION RIBBON",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0BCFF),
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            LazyRow(
                state = ribbonState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("carousel_ribbon"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(games, key = { _, g -> g.id }) { index, game ->
                    val isSelected = index == currentIndex
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = tween(120),
                        label = "ribbon_item_scale"
                    )

                    Surface(
                        modifier = Modifier
                            .width(62.dp)
                            .aspectRatio(0.68f)
                            .scale(scale)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { currentIndex = index }
                            .testTag("carousel_thumb_${game.id}"),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E1A29),
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFFD0BCFF)) else BorderStroke(0.5.dp, Color(0xFF383247))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (game.coverArtUrl.isNotBlank()) {
                                ShelfAsyncImage(
                                    imageUrl = game.coverArtUrl,
                                    contentDescription = game.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(game.coverGradientStart), Color(game.coverGradientEnd))
                                            )
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = game.title,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
