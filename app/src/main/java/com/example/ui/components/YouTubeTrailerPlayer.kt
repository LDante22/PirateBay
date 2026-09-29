package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Game

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeTrailerPlayer(
    youtubeVideoId: String,
    gameTitle: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false
) {
    val context = LocalContext.current
    val cleanVideoId = remember(youtubeVideoId) {
        extractYoutubeVideoId(youtubeVideoId)
    }

    var isPlaying by remember(youtubeVideoId) { mutableStateOf(autoPlay) }
    var isWebLoading by remember(youtubeVideoId) { mutableStateOf(true) }
    var hasRendererError by remember(youtubeVideoId) { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var retryCount by remember(youtubeVideoId) { mutableStateOf(0) }

    val thumbnailUrl = "https://img.youtube.com/vi/$cleanVideoId/hqdefault.jpg"

    DisposableEffect(youtubeVideoId, retryCount) {
        onDispose {
            try {
                webViewInstance?.stopLoading()
                webViewInstance?.loadUrl("about:blank")
                webViewInstance?.destroy()
            } catch (_: Exception) {}
            webViewInstance = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFD0BCFF))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp)),
            color = Color(0xFF1C1B1F),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasRendererError) {
                    // Safe renderer crash recovery view
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF191324))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Playback Notice",
                            tint = Color(0xFFEFB8C8),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Trailer ready in YouTube",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6E1E5)
                            )
                        )
                        Text(
                            text = "Stream directly with full 1080p 60fps fidelity",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCAC4D0)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { openYouTubeExternal(context, cleanVideoId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD0BCFF),
                                    contentColor = Color(0xFF381E72)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("error_open_youtube_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play in YouTube", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    hasRendererError = false
                                    isWebLoading = true
                                    retryCount++
                                    isPlaying = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFCAC4D0))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry In-App", color = Color(0xFFCAC4D0), fontSize = 12.sp)
                            }
                        }
                    }
                } else if (isPlaying && cleanVideoId.isNotEmpty()) {
                    androidx.compose.runtime.key("${cleanVideoId}_$retryCount") {
                        AndroidView(
                            factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                // Enable software rendering if GPU acceleration in emulator container is unstable
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    javaScriptCanOpenWindowsAutomatically = true
                                    // Remove WebView marker (; wv and Version/4.0) so YouTube serves clean HTML5 player without bot/login challenge
                                    val defaultUA = userAgentString ?: ""
                                    if (defaultUA.contains("; wv")) {
                                        userAgentString = defaultUA.replace("; wv", "")
                                    }
                                    safeBrowsingEnabled = false
                                    allowFileAccess = true
                                    allowContentAccess = true
                                }

                                try {
                                    val cookieManager = android.webkit.CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)
                                } catch (_: Exception) {}

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        if (newProgress >= 50) {
                                            isWebLoading = false
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isWebLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isWebLoading = false
                                        }
                                    }

                                    // CRITICAL: Handle renderer crash gracefully without killing the Android app
                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        isWebLoading = false
                                        hasRendererError = true
                                        try {
                                            (view?.parent as? ViewGroup)?.removeView(view)
                                            view?.destroy()
                                        } catch (_: Exception) {}
                                        webViewInstance = null
                                        return true // Prevents host app crash
                                    }
                                }

                                val htmlData = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                        <style>
                                            * { margin: 0; padding: 0; box-sizing: border-box; background: #000; }
                                            body, html { width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                                            .video-container { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
                                            iframe { width: 100%; height: 100%; border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <div class="video-container">
                                            <iframe 
                                                id="player"
                                                src="https://www.youtube-nocookie.com/embed/$cleanVideoId?autoplay=1&playsinline=1&enablejsapi=1&rel=0&modestbranding=1&controls=1&fs=1&iv_load_policy=3&origin=https://www.youtube-nocookie.com" 
                                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen" 
                                                allowfullscreen
                                                referrerpolicy="strict-origin-when-cross-origin">
                                            </iframe>
                                        </div>
                                    </body>
                                    </html>
                                """.trimIndent()

                                loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlData, "text/html", "UTF-8", null)
                                webViewInstance = this
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("youtube_webview")
                    )

                    if (isWebLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFD0BCFF),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    }
                } else {
                    // High-resolution YouTube Video Thumbnail with Play overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                isPlaying = true
                                isWebLoading = true
                            }
                            .testTag("trailer_thumbnail_click")
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Trailer thumbnail for $gameTitle",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient overlay for contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Transparent,
                                            Color(0xFF1C1B1F).copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )

                        // Glowing YouTube play button in Elegant Lavender
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD0BCFF))
                                .shadow(12.dp, CircleShape, spotColor = Color(0xFFD0BCFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play YouTube Trailer",
                                tint = Color(0xFF381E72),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Trailer Header & Official badge
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .background(Color(0xFF1C1B1F).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD0BCFF))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OFFICIAL YOUTUBE TRAILER",
                                color = Color(0xFFE6E1E5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Title on thumbnail bottom
                        Text(
                            text = gameTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6E1E5)
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Row below player
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Trailer Video",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCAC4D0)
                    )
                )
                if (isPlaying && !hasRendererError) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8DEF8))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NOW PLAYING",
                            color = Color(0xFF1D192B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Launch in external YouTube App / Browser
            OutlinedButton(
                onClick = {
                    openYouTubeExternal(context, cleanVideoId)
                },
                modifier = Modifier.testTag("open_youtube_external_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFD0BCFF)
                ),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp,
                    vertical = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open in YouTube",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFD0BCFF)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Open in YouTube",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD0BCFF)
                )
            }
        }
    }
}

/**
 * Dedicated Modal Dialog for Embedded YouTube Game Trailers.
 * Triggered by clicking game items, cover play buttons, or trailer action pills.
 */
@Composable
fun YouTubeTrailerDialog(
    game: Game,
    onDismiss: () -> Unit,
    onOpenDetail: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("youtube_trailer_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1B24)
            ),
            border = BorderStroke(1.5.dp, Color(game.console.accentColorHex).copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Title, Console badge, Year, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(game.console.brandColorHex)
                            ) {
                                Text(
                                    text = "${game.console.emoji} ${game.console.shortName}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (game.releaseYear > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF36343B)
                                ) {
                                    Text(
                                        text = "${game.releaseYear}",
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF5EEF8)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B2930))
                            .testTag("close_trailer_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Trailer",
                            tint = Color(0xFFE6E1E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Embedded YouTube Trailer Player
                YouTubeTrailerPlayer(
                    youtubeVideoId = game.youtubeVideoId,
                    gameTitle = game.title,
                    autoPlay = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Game Quick Info Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (game.genre.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2B2930),
                            border = BorderStroke(1.dp, Color(0xFF49454F))
                        ) {
                            Text(
                                text = "🏷️ ${game.genre}",
                                color = Color(0xFFCAC4D0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2B2930),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Text(
                            text = "${game.status.iconEmoji} ${game.status.label}",
                            color = Color(0xFFE6E1E5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    val emu = game.emulator.ifBlank { game.console.primaryEmulator() }
                    if (emu.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF381E72),
                            border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "🎮 $emu",
                                color = Color(0xFFEADDFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: View Full Specs / Disc & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onOpenDetail != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenDetail()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("trailer_dialog_specs_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6750A4),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View Case & TTB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("trailer_dialog_dismiss_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD0BCFF)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF79747E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extracts YouTube 11-character video ID from common URL variations
 * e.g. https://www.youtube.com/watch?v=dQw4w9WgXcQ
 * or https://youtu.be/dQw4w9WgXcQ
 * or dQw4w9WgXcQ directly
 */
fun extractYoutubeVideoId(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""

    // If it's already an 11-char ID
    if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
        return trimmed
    }

    // Match youtu.be/ID
    val shortPattern = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})")
    shortPattern.find(trimmed)?.let {
        return it.groupValues[1]
    }

    // Match v=ID in query params
    val paramPattern = Regex("[?&]v=([a-zA-Z0-9_-]{11})")
    paramPattern.find(trimmed)?.let {
        return it.groupValues[1]
    }

    // Match embed/ID
    val embedPattern = Regex("embed/([a-zA-Z0-9_-]{11})")
    embedPattern.find(trimmed)?.let {
        return it.groupValues[1]
    }

    return trimmed
}

fun openYouTubeExternal(context: Context, videoId: String) {
    if (videoId.isEmpty()) return
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
    try {
        context.startActivity(appIntent)
    } catch (e: Exception) {
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {}
    }
}
