package com.example.ui.components.case3d

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.metadata.ArtworkValidator
import com.example.data.model.Game
import com.example.ui.components.shelf.ShelfAsyncImage
import kotlin.math.abs
import kotlin.math.cos

/**
 * Physical spine/side face of the 3D game case.
 * Extruded into view when rotated sideways.
 * Supports UV mapping of continuous full wrap inserts or procedural smart fallback
 * with dominant cover color and console branding (never pitch black).
 */
@Composable
fun SpineFace(
    game: Game,
    textureInfo: CaseTextureInfo,
    widthDp: Dp,
    angleY: Float,
    isBackFacing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val console = game.console
    val angleRad = Math.toRadians(angleY.toDouble())
    val ambientShade = (0.15f + 0.30f * (1f - cos(angleRad).toFloat())).coerceIn(0.15f, 0.50f)

    Box(
        modifier = modifier
            .width(widthDp)
            .graphicsLayer {
                if (isBackFacing) {
                    rotationY = 180f
                }
            }
            .background(textureInfo.palette.spineGradient)
            .border(
                BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            )
    ) {
        // Spine plastic hinge groove line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.5.dp)
                .align(Alignment.CenterStart)
                .background(Color.White.copy(alpha = 0.25f))
        )

        // Spine edge shadow
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .align(Alignment.CenterEnd)
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // 1. Full continuous wrap texture slicing (center spine slice)
        if (textureInfo.isFullWrap && textureInfo.wrapBitmap != null) {
            FullWrapSegmentCanvas(
                bitmap = textureInfo.wrapBitmap,
                uvRect = textureInfo.spineUvRect,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (widthDp >= 7.dp) {
            // 2. Smart Procedural Fallback Spine: rich gradient, console badge, vertical title
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Console Spine Logo Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.padding(horizontal = 1.dp)
                ) {
                    Text(
                        text = console.shortName.take(3),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 6.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 1.5.dp, vertical = 1.dp)
                    )
                }

                // Vertical Game Title down the spine
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = game.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 7.5.sp,
                            letterSpacing = 0.8.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .graphicsLayer {
                                rotationZ = 90f
                            }
                            .width(110.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Bottom Spine Publisher / Region Mark
                Surface(
                    color = Color.White.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(1.dp),
                    modifier = Modifier.size(width = 6.dp, height = 8.dp)
                ) {}
            }
        }

        // Ambient lighting shading overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ambientShade))
        )
    }
}

/**
 * Right opening edge of the physical case.
 * Extruded into view when rotated sideways to the left, displaying clear plastic seam
 * and thumb push-to-open latch indent.
 */
@Composable
fun OpeningEdgeFace(
    game: Game,
    textureInfo: CaseTextureInfo,
    widthDp: Dp,
    angleY: Float,
    modifier: Modifier = Modifier
) {
    val console = game.console
    val tintColor = textureInfo.palette.dominantColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .width(widthDp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF16151E),
                        Color(0xFF2A2836),
                        Color.White.copy(alpha = 0.18f),
                        Color(0xFF16151E)
                    )
                )
            )
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)))
    ) {
        // Translucent case plastic tint
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tintColor)
        )

        // Center split seam
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // Thumb push-to-open latch notch
        if (widthDp >= 6.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .height(26.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
            )
        }

        // Ambient shading
        val ambientShade = (0.15f + 0.3f * (abs(angleY) / 75f)).coerceIn(0.15f, 0.5f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ambientShade))
        )
    }
}

/**
 * Front cover face of the physical game case.
 * Remains mapped to the front surface during all 3D rotations, with clear plastic sleeve border
 * and unobscured artwork. Supports continuous full wrap slicing or front artwork.
 */
@Composable
fun FrontCoverFace(
    game: Game,
    textureInfo: CaseTextureInfo,
    angleY: Float,
    enable3DRotation: Boolean,
    onTrailerClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?,
    onFlipToBack: () -> Unit,
    onSpin360: () -> Unit,
    showDeleteButton: Boolean,
    showTrailerButton: Boolean,
    modifier: Modifier = Modifier
) {
    val console = game.console

    Box(
        modifier = modifier
            .background(textureInfo.palette.spineGradient)
    ) {
        // 1. High-resolution Cover Artwork
        if (textureInfo.isFullWrap && textureInfo.wrapBitmap != null) {
            // Front cover is the right slice of the complete physical case wrap
            FullWrapSegmentCanvas(
                bitmap = textureInfo.wrapBitmap,
                uvRect = textureInfo.frontUvRect,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (game.listCover.isNotBlank()) {
            ShelfAsyncImage(
                imageUrl = game.listCover,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Authentic Fallback Cover Design
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color(console.brandColorHex),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = console.shortName,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = game.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = game.genre.ifBlank { console.displayName },
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. Clear Plastic Protective Sleeve Overlay (Physical case border)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    BorderStroke(1.2.dp, Color.White.copy(alpha = 0.22f)),
                    RoundedCornerShape(2.dp)
                )
        )

        // 3. Authentic Console Header Banner
        Surface(
            color = Color(console.brandColorHex).copy(alpha = 0.94f),
            shape = RoundedCornerShape(bottomEnd = 6.dp),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = console.emoji,
                    fontSize = 8.5.sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = console.shortName,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }

        // 4. Top Right: 360° Spin and 3D Back inspection actions (only when 3D rotation is enabled)
        if (enable3DRotation) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clickable { onSpin360() },
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "360°",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.5.sp,
                        modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .clickable { onFlipToBack() },
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.6.dp, Color(0xFFD0BCFF).copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Rotate to Back Cover",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "3D",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }

        // 5. Quick Action Buttons (Trailer, Delete)
        if (showDeleteButton && onDeleteClick != null) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Game",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        if (showTrailerButton && onTrailerClick != null && game.youtubeVideoId.isNotBlank()) {
            IconButton(
                onClick = onTrailerClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color.Red.copy(alpha = 0.88f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Trailer",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Back cover face of the physical game case.
 * Displayed when the case is rotated 180° around the Y-axis.
 * Supports verified back covers, continuous full wrap UV slicing (left side),
 * and dynamic smart procedural fallbacks using colors extracted from front artwork.
 */
@Composable
fun BackCoverFace(
    game: Game,
    textureInfo: CaseTextureInfo,
    angleY: Float,
    onFlipToFront: () -> Unit,
    onSpin360: () -> Unit,
    onImportBackCover: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val console = game.console
    val hasValidBackCover = game.backCoverUrl.isNotBlank() &&
            ArtworkValidator.isBackCoverValidForGame(game, game.backCoverUrl)

    Box(
        modifier = modifier
            .background(textureInfo.palette.backCoverGradient)
    ) {
        // 1. Back Cover Artwork
        if (hasValidBackCover) {
            ShelfAsyncImage(
                imageUrl = game.backCoverUrl,
                contentDescription = "${game.title} Back Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Optional change back cover button
            if (onImportBackCover != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clickable { onImportBackCover() },
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Back Cover",
                            tint = Color.White,
                            modifier = Modifier.size(8.dp)
                        )
                        Text(
                            text = "Change",
                            color = Color.White,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (textureInfo.isFullWrap && textureInfo.wrapBitmap != null) {
            // Renders the authentic back cover directly from the continuous full insert artwork (left side of insert)
            FullWrapSegmentCanvas(
                bitmap = textureInfo.wrapBitmap,
                uvRect = textureInfo.backUvRect,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (onImportBackCover != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clickable { onImportBackCover() },
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Back Cover",
                            tint = Color.White,
                            modifier = Modifier.size(8.dp)
                        )
                        Text(
                            text = "Wrap",
                            color = Color(0xFFD0BCFF),
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Smart Procedural Physical Back Case Layout using Extracted Palette & Console Styling
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // A. Top Console Bar & Official Seal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(console.brandColorHex).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = console.shortName,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 7.5.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                    Text(
                        text = "OFFICIAL CASE ART",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // B. Realistic Gameplay Screenshot Frames / Feature Showcase
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF1E1D2A))
                            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (game.bannerArtUrl.isNotBlank()) {
                            ShelfAsyncImage(
                                imageUrl = game.bannerArtUrl,
                                contentDescription = "Screenshot",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GAMEPLAY", color = Color.White.copy(alpha = 0.85f), fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                Text("1080p HD", color = Color(0xFFD0BCFF), fontSize = 5.5.sp)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF181724))
                            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "★ CAMPAIGN",
                                color = Color(0xFFFFD54F),
                                fontSize = 5.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "★ DUALSHOCK",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 5.5.sp
                            )
                            Text(
                                text = "★ 60 FPS",
                                color = Color(0xFF818CF8),
                                fontSize = 5.5.sp
                            )
                        }
                    }
                }

                // C. Game Synopsis excerpt
                val synopsis = when {
                    game.description.isNotBlank() -> game.description.take(130) + "..."
                    game.notes.isNotBlank() -> game.notes.take(130) + "..."
                    else -> "Experience ${game.title} with revolutionary gameplay mechanics and breathtaking visuals designed exclusively for ${console.displayName}."
                }
                Text(
                    text = synopsis,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 6.sp,
                    lineHeight = 7.5.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // D. Physical Case Specifications Bar & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1-2 Players • Memory Card • Dolby Audio",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 5.sp
                    )
                    Text(
                        text = "|||||||||||||||",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Optional: "+ Import Back Cover" Call to Action button
                if (onImportBackCover != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onImportBackCover() },
                        color = Color(0xFF6750A4).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(0.6.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 2.5.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Import Back Cover Art",
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "+ Import Back Art",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 6.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Clear Plastic Protective Sleeve Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    BorderStroke(1.2.dp, Color.White.copy(alpha = 0.22f)),
                    RoundedCornerShape(2.dp)
                )
        )

        // 3. Quick Action Buttons on Back: "360°" and "FRONT" to rotate back
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clickable { onSpin360() },
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "360°",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.5.sp,
                    modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 2.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .clickable { onFlipToFront() },
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.6.dp, Color(0xFFD0BCFF).copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Rotate to Front Cover",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "FRONT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}
