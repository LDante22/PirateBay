package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.Game
import com.example.ui.components.case3d.BackCoverFace
import com.example.ui.components.case3d.FrontCoverFace
import com.example.ui.components.case3d.OpeningEdgeFace
import com.example.ui.components.case3d.SpineFace
import com.example.ui.components.case3d.rememberCaseTextureInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/**
 * Realistic 3D Physical Game Case.
 *
 * Represents an authentic physical game case (PlayStation / Xbox / Nintendo / Sega).
 * When the user drags across the case:
 * - The entire game case rotates in 3D perspective space (rotationY & rotationX).
 * - Full wraps (Continuous [Back | Spine | Front]) are dynamically UV-mapped and auto-cropped.
 * - Single front covers dynamically generate dominant-color spine and back cover procedural artwork.
 * - Rotating sideways to the right extrudes the physical case depth with console spine branding.
 * - Rotating sideways to the left reveals the clear plastic snap opening edge.
 * - On gesture release, smooth physical spring inertia returns the case to the front or back face.
 */
@Composable
fun Interactive3DGameCard(
    game: Game,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 2f / 3f,
    hasPhysicalSpine: Boolean = true,
    enable3DRotation: Boolean = false,
    onClick: (() -> Unit)? = null,
    onTrailerClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onImportBackCover: (() -> Unit)? = null,
    showDeleteButton: Boolean = false,
    showTrailerButton: Boolean = true,
    showDetailsButton: Boolean = false,
    initialFlipped: Boolean = false,
    spinTrigger: Int = 0,
    flipTrigger: Int = 0
) {
    var rotationAngleY by remember(game.id) { mutableFloatStateOf(if (initialFlipped && enable3DRotation) 180f else 0f) }
    var rotationAngleX by remember(game.id) { mutableFloatStateOf(0f) }
    var isDragging by remember(game.id) { mutableStateOf(false) }
    var totalDragDistance by remember(game.id) { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Resolved UV texture info, aspect ratio scaling, and smart fallback palette
    val textureInfo = rememberCaseTextureInfo(game)

    // Smooth physical rotation with spring damping
    val animatedRotationY by animateFloatAsState(
        targetValue = if (enable3DRotation) rotationAngleY else 0f,
        animationSpec = if (isDragging) {
            spring(stiffness = Spring.StiffnessHigh, dampingRatio = 0.95f)
        } else {
            spring(stiffness = 260f, dampingRatio = 0.70f)
        },
        label = "case_3d_rot_y_${game.id}"
    )

    val animatedRotationX by animateFloatAsState(
        targetValue = if (enable3DRotation) rotationAngleX else 0f,
        animationSpec = if (isDragging) {
            spring(stiffness = Spring.StiffnessHigh, dampingRatio = 0.95f)
        } else {
            spring(stiffness = 260f, dampingRatio = 0.70f)
        },
        label = "case_3d_rot_x_${game.id}"
    )

    // Tactile press & drag responsive scale
    val pressScale by animateFloatAsState(
        targetValue = if (enable3DRotation) {
            if (isDragging) 0.965f else 1f
        } else {
            if (isPressed) 0.97f else 1f
        },
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "case_3d_press_scale_${game.id}"
    )

    // Trigger programmatic 360° spin (only active when 3D rotation is enabled)
    LaunchedEffect(spinTrigger, enable3DRotation) {
        if (enable3DRotation && spinTrigger > 0) {
            rotationAngleY += 360f
        }
    }

    // Trigger programmatic flip between Front and Back face (only active when 3D rotation is enabled)
    LaunchedEffect(flipTrigger, enable3DRotation) {
        if (enable3DRotation && flipTrigger > 0) {
            val norm = ((rotationAngleY % 360f) + 360f) % 360f
            val turns = Math.floor(rotationAngleY.toDouble() / 360.0).toFloat()
            rotationAngleY = if (norm in 90f..270f) {
                turns * 360f
            } else {
                turns * 360f + 180f
            }
        }
    }

    // Calculate normalized angle in [0, 360) to determine which hemisphere is visible
    val normalizedAngleY = if (enable3DRotation) {
        ((animatedRotationY % 360f) + 360f) % 360f
    } else {
        0f
    }
    val isBackVisible = enable3DRotation && normalizedAngleY in 90f..270f

    // Physical case depth (18dp for collectible physical game cases)
    val maxCaseDepthDp = 18.dp
    val angleYRad = Math.toRadians(animatedRotationY.toDouble())
    val sinAngle = sin(angleYRad).toFloat()
    val absSinY = abs(sinAngle)
    val projectedDepthDp = (maxCaseDepthDp * absSinY).coerceAtLeast(0.dp)

    // Dynamic ground contact shadow offset in opposite direction of 3D rotation
    val shadowOffsetX = (-sinAngle * 14f).dp
    val shadowElevation = (6f + absSinY * 9f).dp

    val cardModifier = if (enable3DRotation) {
        modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .testTag("3d_game_case_${game.id}")
            .pointerInput(game.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        totalDragDistance = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        // If tapped without significant drag, open details
                        if (totalDragDistance < 8f && onClick != null) {
                            onClick()
                        } else {
                            // Settle smoothly to nearest front or back face with forward progression
                            val norm = ((rotationAngleY % 360f) + 360f) % 360f
                            val turns = Math.floor(rotationAngleY.toDouble() / 360.0).toFloat()
                            rotationAngleY = if (norm in 90f..270f) {
                                turns * 360f + 180f
                            } else if (norm > 270f) {
                                (turns + 1) * 360f
                            } else {
                                turns * 360f
                            }
                        }
                        rotationAngleX = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        rotationAngleX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragDistance += abs(dragAmount.x) + abs(dragAmount.y)
                        // Smoothly rotate 3D box along Y (horizontal) and subtle pitch tilt along X (vertical)
                        rotationAngleY += dragAmount.x * 0.82f
                        rotationAngleX = (rotationAngleX - dragAmount.y * 0.16f).coerceIn(-14f, 14f)
                    }
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (totalDragDistance < 8f && onClick != null) {
                        onClick()
                    }
                }
            )
    } else {
        val base = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .testTag("3d_game_case_${game.id}")
        if (onClick != null) {
            base.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
        } else {
            base
        }
    }

    Box(
        modifier = cardModifier,
        contentAlignment = Alignment.Center
    ) {
        // 1. Dynamic Contact Shadow Grounded Behind the Physical Case
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .offset(x = shadowOffsetX, y = 6.dp)
                .graphicsLayer {
                    this.rotationY = animatedRotationY * 0.45f
                    this.alpha = 0.45f + absSinY * 0.25f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        // 2. The 3D Physical Game Case Block (Front Cover / Back Cover + Side Thickness/Spine)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.scaleX = pressScale
                    this.scaleY = pressScale
                    this.rotationY = animatedRotationY
                    this.rotationX = animatedRotationX
                    this.cameraDistance = 14f * density
                    this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                    this.shadowElevation = shadowElevation.toPx()
                    this.shape = RoundedCornerShape(
                        topStart = 3.dp,
                        bottomStart = 3.dp,
                        topEnd = 5.dp,
                        bottomEnd = 5.dp
                    )
                    this.clip = true
                }
                .background(textureInfo.palette.darkGradientEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. PHYSICAL SPINE (Visible when rotated to the right, sinAngle > 0.05)
            if (sinAngle > 0.05f) {
                SpineFace(
                    game = game,
                    textureInfo = textureInfo,
                    widthDp = projectedDepthDp,
                    angleY = animatedRotationY,
                    isBackFacing = isBackVisible,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            // B. MAIN FACE: Front Cover or Back Cover
            if (isBackVisible) {
                BackCoverFace(
                    game = game,
                    textureInfo = textureInfo,
                    angleY = animatedRotationY,
                    onFlipToFront = {
                        coroutineScope.launch {
                            val turns = Math.round(rotationAngleY / 360f)
                            rotationAngleY = turns * 360f
                        }
                    },
                    onSpin360 = {
                        coroutineScope.launch {
                            rotationAngleY += 360f
                        }
                    },
                    onImportBackCover = onImportBackCover,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            // Counter-rotate 180° around Y so that the back cover artwork and text
                            // are upright, unmirrored, and fully readable!
                            this.rotationY = 180f
                        }
                )
            } else {
                FrontCoverFace(
                    game = game,
                    textureInfo = textureInfo,
                    angleY = animatedRotationY,
                    enable3DRotation = enable3DRotation,
                    onTrailerClick = onTrailerClick,
                    onDeleteClick = onDeleteClick,
                    onFlipToBack = {
                        coroutineScope.launch {
                            val turns = Math.floor(rotationAngleY.toDouble() / 360.0).toFloat()
                            rotationAngleY = turns * 360f + 180f
                        }
                    },
                    onSpin360 = {
                        coroutineScope.launch {
                            rotationAngleY += 360f
                        }
                    },
                    showDeleteButton = showDeleteButton,
                    showTrailerButton = showTrailerButton,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // C. PHYSICAL OPENING EDGE (Visible when rotated to the left, sinAngle < -0.05)
            if (sinAngle < -0.05f) {
                OpeningEdgeFace(
                    game = game,
                    textureInfo = textureInfo,
                    widthDp = projectedDepthDp,
                    angleY = animatedRotationY,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}
