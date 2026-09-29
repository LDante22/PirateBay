package com.example.ui.components.shelf

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Shared Coil ImageLoader singleton configured specifically for the Digital Shelf.
 * Provides a dedicated memory cache (30% heap limit) and disk cache with aggressive
 * memory retention and crossfades to prevent layout-switching flicker.
 */
object ShelfImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.30)
                        .strongReferencesEnabled(true)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("shelf_image_cache"))
                        .maxSizeBytes(250L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .crossfade(200)
                .respectCacheHeaders(false)
                .allowHardware(true)
                .build()
                .also { instance = it }
        }
    }
}

/**
 * Builds an [ImageRequest] configured with memory caching, disk caching, and crossfade transitions.
 * Standardizes cache keys on the raw image URL so that switching between different shelf views
 * instantly hits the in-memory bitmap cache without flicker or reload delays.
 */
@Composable
fun rememberShelfImageRequest(
    imageUrl: String,
    crossfadeDurationMs: Int = 200
): ImageRequest {
    val context = LocalContext.current
    return remember(imageUrl, crossfadeDurationMs) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(imageUrl)
            .diskCacheKey(imageUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .crossfade(crossfadeDurationMs)
            .allowHardware(true)
            .build()
    }
}

/**
 * High-performance, flicker-free AsyncImage composable for Digital Shelf views.
 * Displays a clean loading indicator while downloading artwork.
 */
@Composable
fun ShelfAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    crossfadeDurationMs: Int = 200
) {
    val request = rememberShelfImageRequest(
        imageUrl = imageUrl,
        crossfadeDurationMs = crossfadeDurationMs
    )
    val context = LocalContext.current
    val imageLoader = remember { ShelfImageLoader.getImageLoader(context) }

    SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        modifier = modifier
    ) {
        val state = painter.state
        if (state is AsyncImagePainter.State.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        } else {
            SubcomposeAsyncImageContent()
        }
    }
}

