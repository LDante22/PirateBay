package com.example.ui.components.case3d

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.ui.components.shelf.ShelfImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Palette extracted from game artwork and tailored to the platform console.
 * Ensures the spine and back cover always have rich, authentic gradients,
 * never appearing pitch black.
 */
data class CasePalette(
    val dominantColor: Color,
    val darkGradientEnd: Color,
    val platformBrand: Color,
    val platformAccent: Color,
    val spineGradient: Brush,
    val backCoverGradient: Brush
)

/**
 * Resolved texture metadata for physical 3D case rendering.
 * Supports UV mapping of continuous [Back | Spine | Front] wraps
 * and smart procedural fallbacks for front-only covers.
 */
data class CaseTextureInfo(
    val wrapBitmap: ImageBitmap?,
    val frontBitmap: ImageBitmap?,
    val isFullWrap: Boolean,
    val backUvRect: Rect,
    val spineUvRect: Rect,
    val frontUvRect: Rect,
    val palette: CasePalette
)

/**
 * Fast pixel sampling palette extractor.
 * Extracts dominant vibrant chromatic values from cover art and blends them with
 * console brand identity (e.g., PS2 blue, Xbox green, Switch red).
 */
fun extractCasePalette(bitmap: Bitmap?, console: GameConsole): CasePalette {
    val brand = Color(console.brandColorHex)
    val accent = Color(console.accentColorHex)

    var dominant = brand
    var darkEnd = Color(0xFF14131A)

    if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
        try {
            val w = bitmap.width
            val h = bitmap.height
            val stepX = (w / 12).coerceAtLeast(1)
            val stepY = (h / 12).coerceAtLeast(1)

            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var count = 0

            for (x in stepX until w step stepX) {
                for (y in stepY until h step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val a = android.graphics.Color.alpha(pixel)
                    if (a < 128) continue

                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)

                    val brightness = (r * 299 + g * 587 + b * 114) / 1000
                    if (brightness in 30..230) {
                        totalR += r
                        totalG += g
                        totalB += b
                        count++
                    }
                }
            }

            if (count > 0) {
                val avgR = (totalR / count).toInt().coerceIn(0, 255)
                val avgG = (totalG / count).toInt().coerceIn(0, 255)
                val avgB = (totalB / count).toInt().coerceIn(0, 255)

                dominant = Color(avgR, avgG, avgB)
                darkEnd = Color(
                    (avgR * 0.20f).toInt().coerceIn(10, 50),
                    (avgG * 0.20f).toInt().coerceIn(10, 50),
                    (avgB * 0.20f).toInt().coerceIn(15, 60)
                )
            }
        } catch (_: Exception) {
            // Graceful fallback to console brand color
        }
    }

    // Platform-specific gradient adjustments
    val platformTint = when (console) {
        GameConsole.PS2 -> Color(0xFF0A2540) // PS2 Navy Blue
        GameConsole.PS3 -> Color(0xFF1E1B29) // PS3 Glossy Charcoal
        GameConsole.PS4, GameConsole.PS5 -> Color(0xFF003791) // PlayStation Royal Blue
        GameConsole.PSP, GameConsole.PS_VITA -> Color(0xFF0F172A)
        GameConsole.XBOX, GameConsole.XBOX_360, GameConsole.XBOX_ONE -> Color(0xFF0E3D12) // Xbox Forest Green
        GameConsole.SWITCH -> Color(0xFF8B0000) // Switch Deep Crimson
        GameConsole.GAMECUBE -> Color(0xFF2E0854) // GameCube Indigo
        GameConsole.WII, GameConsole.WII_U -> Color(0xFF0F2B48) // Wii Cyan Midnight
        else -> brand.copy(alpha = 0.85f)
    }

    val spineGradient = Brush.verticalGradient(
        colors = listOf(
            dominant.copy(alpha = 0.95f),
            platformTint,
            darkEnd
        )
    )

    val backCoverGradient = Brush.verticalGradient(
        colors = listOf(
            dominant.copy(alpha = 0.88f),
            platformTint.copy(alpha = 0.92f),
            darkEnd
        )
    )

    return CasePalette(
        dominantColor = dominant,
        darkGradientEnd = darkEnd,
        platformBrand = brand,
        platformAccent = accent,
        spineGradient = spineGradient,
        backCoverGradient = backCoverGradient
    )
}

/**
 * Resolves texture info, aspect ratio analysis, UV coordinates, and color palette
 * for the 3D physical game case.
 */
@Composable
fun rememberCaseTextureInfo(game: Game): CaseTextureInfo {
    val context = LocalContext.current
    var wrapAndroidBitmap by remember(game.completeCaseArtwork) { mutableStateOf<Bitmap?>(null) }
    var frontAndroidBitmap by remember(game.frontCover, game.listCover) { mutableStateOf<Bitmap?>(null) }

    // Load full wrap bitmap if available
    LaunchedEffect(game.completeCaseArtwork) {
        if (game.completeCaseArtwork.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ShelfImageLoader.getImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(game.completeCaseArtwork)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult && result.drawable is BitmapDrawable) {
                        wrapAndroidBitmap = (result.drawable as BitmapDrawable).bitmap
                    }
                } catch (_: Exception) {
                    wrapAndroidBitmap = null
                }
            }
        } else {
            wrapAndroidBitmap = null
        }
    }

    // Load front cover bitmap if available
    val frontUrl = game.frontCover.ifBlank { game.listCover }
    LaunchedEffect(frontUrl) {
        if (frontUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ShelfImageLoader.getImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(frontUrl)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult && result.drawable is BitmapDrawable) {
                        frontAndroidBitmap = (result.drawable as BitmapDrawable).bitmap
                    }
                } catch (_: Exception) {
                    frontAndroidBitmap = null
                }
            }
        } else {
            frontAndroidBitmap = null
        }
    }

    // Determine if completeCaseArtwork is a continuous horizontal wrap (AR >= 1.15)
    val wrapBmp = wrapAndroidBitmap
    val isFullWrap = if (wrapBmp != null && wrapBmp.width > 0 && wrapBmp.height > 0) {
        val ar = wrapBmp.width.toFloat() / wrapBmp.height.toFloat()
        ar >= 1.15f
    } else {
        false
    }

    // Proportional spine width ratio according to physical console casing
    val spineRatio = when (game.console) {
        GameConsole.SWITCH -> 0.046f
        GameConsole.PS2, GameConsole.PS3, GameConsole.PS4, GameConsole.PS5 -> 0.055f
        GameConsole.XBOX, GameConsole.XBOX_360, GameConsole.XBOX_ONE -> 0.055f
        GameConsole.GAMECUBE, GameConsole.WII, GameConsole.WII_U -> 0.060f
        else -> 0.058f
    }

    // UV Bounds: Center spine with left back cover and right front cover
    val spineLeft = (0.5f - spineRatio / 2f).coerceIn(0.40f, 0.48f)
    val spineRight = (0.5f + spineRatio / 2f).coerceIn(0.52f, 0.60f)

    val backUv = Rect(0.0f, 0.0f, spineLeft, 1.0f)
    val spineUv = Rect(spineLeft, 0.0f, spineRight, 1.0f)
    val frontUv = Rect(spineRight, 0.0f, 1.0f, 1.0f)

    // Palette extracted from whichever bitmap is available (front or wrap)
    val paletteBitmap = frontAndroidBitmap ?: wrapAndroidBitmap
    val palette = remember(paletteBitmap, game.console) {
        extractCasePalette(paletteBitmap, game.console)
    }

    val wrapImageBitmap = remember(wrapAndroidBitmap) { wrapAndroidBitmap?.asImageBitmap() }
    val frontImageBitmap = remember(frontAndroidBitmap) { frontAndroidBitmap?.asImageBitmap() }

    return CaseTextureInfo(
        wrapBitmap = wrapImageBitmap,
        frontBitmap = frontImageBitmap,
        isFullWrap = isFullWrap,
        backUvRect = backUv,
        spineUvRect = spineUv,
        frontUvRect = frontUv,
        palette = palette
    )
}

/**
 * Custom Canvas renderer that slices a UV sub-rectangle from a full wrap bitmap,
 * calculates aspect-ratio auto-crop, and scales it to fill the destination face
 * without gaps, overflow, or stretching.
 */
@Composable
fun FullWrapSegmentCanvas(
    bitmap: ImageBitmap,
    uvRect: Rect,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f || bitmap.width <= 0 || bitmap.height <= 0) return@Canvas

        val bmpW = bitmap.width
        val bmpH = bitmap.height

        val rawLeft = (uvRect.left * bmpW).toInt().coerceIn(0, bmpW - 1)
        val rawTop = (uvRect.top * bmpH).toInt().coerceIn(0, bmpH - 1)
        val rawRight = (uvRect.right * bmpW).toInt().coerceIn(rawLeft + 1, bmpW)
        val rawBottom = (uvRect.bottom * bmpH).toInt().coerceIn(rawTop + 1, bmpH)

        val rawSliceW = (rawRight - rawLeft).toFloat().coerceAtLeast(1f)
        val rawSliceH = (rawBottom - rawTop).toFloat().coerceAtLeast(1f)

        val targetAspect = size.width / size.height
        val sliceAspect = rawSliceW / rawSliceH

        // Aspect ratio auto-crop adjustment so that content fits target face exactly
        val (finalLeft, finalTop, finalW, finalH) = if (contentScale == ContentScale.Crop) {
            if (sliceAspect > targetAspect) {
                // Slice is wider than target: crop horizontally around center
                val neededW = rawSliceH * targetAspect
                val deltaX = (rawSliceW - neededW) / 2f
                val adjL = (rawLeft + deltaX).toInt().coerceIn(0, bmpW - 1)
                val adjW = neededW.toInt().coerceIn(1, bmpW - adjL)
                arrayOf(adjL, rawTop, adjW, rawSliceH.toInt())
            } else {
                // Slice is taller than target: crop vertically around center
                val neededH = rawSliceW / targetAspect
                val deltaY = (rawSliceH - neededH) / 2f
                val adjT = (rawTop + deltaY).toInt().coerceIn(0, bmpH - 1)
                val adjH = neededH.toInt().coerceIn(1, bmpH - adjT)
                arrayOf(rawLeft, adjT, rawSliceW.toInt(), adjH)
            }
        } else {
            arrayOf(rawLeft, rawTop, rawSliceW.toInt(), rawSliceH.toInt())
        }

        drawImage(
            image = bitmap,
            srcOffset = IntOffset(finalLeft, finalTop),
            srcSize = IntSize(finalW, finalH),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.High
        )
    }
}
