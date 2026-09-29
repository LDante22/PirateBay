package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Game
import com.example.data.model.GameConsole

@Composable
fun DiscCoverCard(
    game: Game,
    modifier: Modifier = Modifier,
    showDiscPeeking: Boolean = false,
    isInteractive: Boolean = true,
    onTrailerClick: (() -> Unit)? = null
) {
    val console = game.console
    val consoleBrandColor = Color(console.brandColorHex)
    val consoleAccentColor = Color(console.accentColorHex)

    Box(
        modifier = modifier
            .testTag("disc_cover_${game.id}")
            .padding(4.dp)
    ) {
        // Physical Game Case Front Cover (Keep only the case cover photo)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(getCaseAspectRatio(console))
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                    spotColor = consoleAccentColor.copy(alpha = 0.4f)
                ),
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 6.dp, bottomEnd = 6.dp),
            color = Color(0xFF131B2E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A374F))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Spine gradient highlight (3D plastic case reflection on left edge)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 40f
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    // Console Header Banner
                    ConsoleCaseHeader(console = console)

                    // Cover Artwork Artwork Body
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(game.coverGradientStart),
                                        Color(game.coverGradientEnd)
                                    )
                                )
                            )
                    ) {
                        if (game.coverArtUrl.isNotBlank()) {
                            com.example.ui.components.shelf.ShelfAsyncImage(
                                imageUrl = game.coverArtUrl,
                                contentDescription = "Cover art for ${game.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Subtle geometric / light reflection texture
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(game.coverAccentColor).copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                                        radius = size.width * 0.7f
                                    )
                                )
                                // Diagonal shine
                                drawLine(
                                    color = Color.White.copy(alpha = 0.08f),
                                    start = Offset(0f, size.height * 0.8f),
                                    end = Offset(size.width, size.height * 0.2f),
                                    strokeWidth = 12f
                                )
                            }
                        }

                        // Play Trailer Indicator Badge on Cover (Clickable)
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.5.dp, Color(game.coverAccentColor), CircleShape)
                                .then(
                                    if (onTrailerClick != null) {
                                        Modifier
                                            .clickable(onClick = onTrailerClick)
                                            .testTag("play_trailer_badge_${game.id}")
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch Trailer for ${game.title}",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Game Information on Cover bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f),
                                            Color.Black.copy(alpha = 0.95f)
                                        )
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = game.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp
                                ),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = game.genre,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format("%.1f", game.userRating),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    )
                                }
                            }
                        }

                        // Emulator badge in top-right corner
                        val emu = game.emulator.ifBlank { console.primaryEmulator() }
                        if (emu.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.75f),
                                        RoundedCornerShape(3.dp)
                                    )
                                    .border(0.5.dp, Color(console.accentColorHex).copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = emu,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Bottom Case Plastic Bar
                    ConsoleCaseFooter(console = console)
                }

                // Plastic Case Gloss Sheen Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(300f, 600f)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun ConsoleCaseHeader(console: GameConsole) {
    when (console) {
        GameConsole.PS2 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color(0xFF0A0F1D))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PlayStation 2",
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "PAL / NTSC",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.PS3 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF0284C7)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PlayStation 3",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Blu-ray Disc",
                    color = Color(0xFF38BDF8),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.PS4 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF003791),
                                Color(0xFF00205B),
                                Color(0xFF003791)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PlayStation®4",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "PS4™",
                    color = Color(0xFF60A5FA),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        GameConsole.PS5 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Color.White)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PlayStation®5",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Ultra HD Blu-ray",
                    color = Color(0xFF003791),
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.GBA -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF312E81),
                                Color(0xFF4C1D95),
                                Color(0xFF1E1B4B)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GAME BOY ADVANCE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "GBA",
                    color = Color(0xFFC084FC),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.N64 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NINTENDO 64",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "ONLY FOR",
                    color = Color(0xFFEF4444),
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        GameConsole.NINTENDO_DS -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color(0xFF0284C7))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nintendo DS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                )
                Text(
                    text = "DS",
                    color = Color(0xFFBAE6FD),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.XBOX -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Color(0xFF14532D))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "XBOX",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "DVD",
                    color = Color(0xFF4ADE80),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.XBOX_360 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF16A34A),
                                Color(0xFF22C55E),
                                Color(0xFF15803D)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "XBOX 360",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "LIVE",
                    color = Color(0xFFFEF08A),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        GameConsole.XBOX_ONE -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Color(0xFF065F46))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "XBOX ONE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Blu-ray",
                    color = Color(0xFF6EE7B7),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.PSP -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color.White)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PSP™",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "UMD",
                    color = Color(0xFF475569),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.PS_VITA -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color(0xFF003791))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PlayStation®Vita",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = "NVG",
                    color = Color(0xFF38BDF8),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.GAMECUBE -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Color(0xFF432C7A))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NINTENDO GAMECUBE™",
                    color = Color(0xFFE9D5FF),
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "🟣",
                    fontSize = 9.sp
                )
            }
        }
        GameConsole.WII -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color.White)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wii",
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
                Text(
                    text = "Nintendo",
                    color = Color(0xFF64748B),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.WII_U -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0077B6),
                                Color(0xFF00B4D8),
                                Color(0xFF0096C7)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Wii U",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "™",
                        color = Color(0xFFCAF0F8),
                        fontSize = 7.sp
                    )
                }
                Text(
                    text = "Nintendo",
                    color = Color(0xFFCAF0F8),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.SWITCH -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Color(0xFFE11D48))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NINTENDO SWITCH™",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "🔴",
                    fontSize = 9.sp
                )
            }
        }
        GameConsole.NINTENDO_3DS -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NINTENDO 3DS™",
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                )
                Text(
                    text = "3D",
                    color = Color(0xFF475569),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GameConsole.PC -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PC CD-ROM / STEAM",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "DVD",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(Color(console.brandColorHex))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = console.bannerText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun ConsoleCaseFooter(console: GameConsole) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color(0xFF0B0F19))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = console.mediaType,
                color = Color(0xFF64748B),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "WISHLIST",
                color = Color(console.accentColorHex),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getCaseAspectRatio(console: GameConsole): Float {
    return when (console) {
        GameConsole.PSP -> 0.62f // Tall slim UMD case
        GameConsole.PS_VITA -> 0.68f // Small Vita case
        GameConsole.NINTENDO_3DS -> 0.78f // Squarish 3DS case
        GameConsole.NINTENDO_DS -> 0.82f // Squarish DS case
        GameConsole.GBA -> 0.75f // GBA box ratio
        GameConsole.N64 -> 0.72f // N64 box ratio
        GameConsole.SWITCH -> 0.60f // Tall slender Switch case
        GameConsole.GAMECUBE -> 0.70f // GameCube DVD case
        GameConsole.PS4, GameConsole.PS5, GameConsole.XBOX_ONE -> 0.70f // Slim Blu-ray case
        GameConsole.XBOX, GameConsole.XBOX_360 -> 0.71f // DVD case
        else -> 0.70f // Standard DVD/Blu-ray case ratio
    }
}

/**
 * High fidelity physical Optical Disc / Cartridge representation
 * Complete with metallic sheen, rainbow laser diffraction, and spindle hub hole.
 */
@Composable
fun PhysicalDiscView(
    game: Game,
    modifier: Modifier = Modifier,
    isSpinning: Boolean = false,
    discSize: Dp = 200.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val rotation by if (isSpinning) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(1000)),
            label = "static_rotation"
        )
    }

    val console = game.console

    if (console.isDisc) {
        // Optical Disc (CD, DVD, UMD, Blu-ray, Mini-DVD)
        Box(
            modifier = modifier
                .size(discSize)
                .rotate(rotation)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = Color(console.accentColorHex).copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // Outer Disc Canvas with Metallic & Holographic Laser Tracks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer silver rim
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFCBD5E1),
                            Color(0xFF64748B),
                            Color(0xFFE2E8F0),
                            Color(0xFF475569),
                            Color(0xFF94A3B8),
                            Color(0xFFCBD5E1)
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center
                )

                // Laser Diffraction Data Tracks Layer (Rainbow Sheen)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.4f),
                            Color(0xFFA855F7).copy(alpha = 0.4f),
                            Color(0xFFF43F5E).copy(alpha = 0.4f),
                            Color(0xFFFACC15).copy(alpha = 0.4f),
                            Color(0xFF10B981).copy(alpha = 0.4f),
                            Color(0xFF38BDF8).copy(alpha = 0.4f)
                        ),
                        center = center
                    ),
                    radius = radius * 0.96f,
                    center = center
                )

                // Disc Printed Silk-Screen Top Half (Art color)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(game.coverGradientStart).copy(alpha = 0.85f),
                            Color(game.coverGradientEnd).copy(alpha = 0.85f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    radius = radius * 0.92f,
                    center = center
                )

                // Disc concentric groove rings
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius * 0.85f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = radius * 0.70f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius * 0.52f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Inner Spindle Clear Ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color(0xFF94A3B8).copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        ),
                        center = center,
                        radius = radius * 0.40f
                    ),
                    radius = radius * 0.38f,
                    center = center
                )

                // Inner Center Hole (transparent spindle hole)
                drawCircle(
                    color = Color(0xFF090D16),
                    radius = radius * 0.16f,
                    center = center
                )

                // Center hole plastic border
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = radius * 0.16f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            // Top Silk-Screen Text / Console Badge
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(discSize * 0.12f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Disc Label
                Text(
                    text = console.bannerText,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (discSize.value * 0.055f).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                // Bottom Disc Game Title
                Text(
                    text = game.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (discSize.value * 0.048f).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // Cartridge / Game Card (Switch, PS Vita, 3DS)
        PhysicalCartridgeView(
            game = game,
            modifier = modifier,
            cartSize = discSize
        )
    }
}

@Composable
fun PhysicalCartridgeView(
    game: Game,
    modifier: Modifier = Modifier,
    cartSize: Dp = 200.dp
) {
    val console = game.console
    val cartColor = when (console) {
        GameConsole.SWITCH -> Color(0xFF1E1E1E)
        GameConsole.PS_VITA -> Color(0xFF003791)
        GameConsole.NINTENDO_3DS -> Color(0xFFE2E8F0)
        GameConsole.NINTENDO_DS -> Color(0xFF334155)
        GameConsole.GBA -> Color(0xFF312E81)
        GameConsole.N64 -> Color(0xFF475569)
        else -> Color(0xFF1E293B)
    }

    Surface(
        modifier = modifier
            .size(width = cartSize * 0.75f, height = cartSize)
            .shadow(10.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = cartColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(console.accentColorHex))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cartridge Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(console.brandColorHex), RoundedCornerShape(4.dp))
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = console.shortName,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }

            // Cartridge Sticker
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(game.coverGradientStart),
                                Color(game.coverGradientEnd)
                            )
                        )
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = game.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = game.genre,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Cartridge Gold Pins Notch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFEAB308), RoundedCornerShape(2.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(6) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(6.dp)
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}
