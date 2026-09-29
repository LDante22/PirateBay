package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.hltb.HltbLoadingStatus
import com.example.data.model.Game

@Composable
fun TimeToBeatCard(
    game: Game,
    loadingStatus: HltbLoadingStatus = HltbLoadingStatus.IDLE,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consoleAccent = Color(game.console.accentColorHex)
    val isLoading = loadingStatus == HltbLoadingStatus.LOADING

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_time_to_beat_${game.id}"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B1826),
        border = BorderStroke(1.dp, Color(0xFF2E2A3E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title, Source badge, Refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Time to Beat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1F5F9),
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Source: HowLongToBeat",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Refresh Button
                val infiniteTransition = rememberInfiniteTransition(label = "hltb_spin")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "spin_angle"
                )

                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_refresh_hltb")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Time to Beat",
                        tint = if (isLoading) Color(0xFFA5B4FC) else Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(18.dp)
                            .then(if (isLoading) Modifier.rotate(rotation) else Modifier)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body content based on state
            AnimatedContent(
                targetState = when {
                    isLoading -> "LOADING"
                    game.hasHltbData || (game.hltbSyncStatus == "SYNCED" && game.hasHltbData) -> "DATA"
                    game.hltbSyncStatus == "NOT_FOUND" -> "NOT_FOUND"
                    game.hltbSyncStatus == "UNRESOLVED" -> "UNRESOLVED"
                    game.hltbSyncStatus == "ERROR" -> "ERROR"
                    else -> "UNAVAILABLE"
                },
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "hltb_state_transition"
            ) { state ->
                when (state) {
                    "LOADING" -> {
                        HltbLoadingView()
                    }
                    "DATA" -> {
                        HltbDataView(game = game, consoleAccent = consoleAccent)
                    }
                    "NOT_FOUND" -> {
                        HltbMessageView(
                            icon = Icons.Default.SearchOff,
                            iconTint = Color(0xFF94A3B8),
                            message = "Game not found on HowLongToBeat",
                            subMessage = "No matching completion records found for this title",
                            onRetry = onRefresh
                        )
                    }
                    "UNRESOLVED" -> {
                        HltbMessageView(
                            icon = Icons.Default.Warning,
                            iconTint = Color(0xFFFBBF24),
                            message = "Time to Beat unavailable",
                            subMessage = "Multiple ambiguous results detected — no playtime attached",
                            onRetry = onRefresh
                        )
                    }
                    "ERROR" -> {
                        HltbMessageView(
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFFF87171),
                            message = "Time to Beat temporarily unavailable",
                            subMessage = "Check internet connection or try again later",
                            onRetry = onRefresh
                        )
                    }
                    else -> {
                        HltbMessageView(
                            icon = Icons.Default.HourglassEmpty,
                            iconTint = Color(0xFF94A3B8),
                            message = "Time to Beat unavailable",
                            subMessage = "Tap refresh to search completion times",
                            onRetry = onRefresh
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HltbLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp,
            color = Color(0xFF818CF8)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Loading playtime...",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun HltbDataView(
    game: Game,
    consoleAccent: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Story Stat Card
            StatMetricPill(
                modifier = Modifier.weight(1f),
                label = "Main Story",
                timeValue = game.formattedMainStory,
                accentColor = Color(0xFF38BDF8),
                bgTint = Color(0xFF0F2642),
                tag = "metric_main_story"
            )

            // Main + Extras Stat Card
            StatMetricPill(
                modifier = Modifier.weight(1f),
                label = "Main + Extras",
                timeValue = game.formattedMainExtra,
                accentColor = Color(0xFFFBBF24),
                bgTint = Color(0xFF3B2D0B),
                tag = "metric_main_extras"
            )

            // 100% Completionist Stat Card
            StatMetricPill(
                modifier = Modifier.weight(1f),
                label = "100%",
                timeValue = game.formattedCompletionist,
                accentColor = Color(0xFF34D399),
                bgTint = Color(0xFF0B3322),
                tag = "metric_completionist"
            )
        }

        if (game.hltbName.isNotBlank() && !game.hltbName.equals(game.title, ignoreCase = true)) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF13101E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Matched: ${game.hltbName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricPill(
    label: String,
    timeValue: String,
    accentColor: Color,
    bgTint: Color,
    modifier: Modifier = Modifier,
    tag: String = ""
) {
    Surface(
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(12.dp),
        color = bgTint.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = timeValue,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HltbMessageView(
    icon: ImageVector,
    iconTint: Color,
    message: String,
    subMessage: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.15f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp
                )
            )
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp
                )
            )
        }

        TextButton(
            onClick = onRetry,
            modifier = Modifier.testTag("btn_retry_hltb")
        ) {
            Text(
                text = "Retry",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFA5B4FC),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
