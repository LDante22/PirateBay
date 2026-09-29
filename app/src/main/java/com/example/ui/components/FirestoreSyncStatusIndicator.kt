package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.UserProfile
import com.example.data.sync.CloudSyncState
import com.example.data.sync.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Format relative elapsed time since last sync (e.g. "Just now", "2m ago", "Today at 7:32 AM").
 */
fun formatRelativeSyncTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Never synced"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 0) return "Just now"

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 30 -> "Just now"
        seconds < 60 -> "${seconds}s ago"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Format exact human-readable date & time for last sync (e.g. "Aug 25, 2026 • 7:32:15 AM").
 */
fun formatExactSyncTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Awaiting first sync"
    return SimpleDateFormat("MMM d, yyyy • h:mm:ss a", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Visual color palette and icons associated with each CloudSyncState.
 */
data class SyncStateVisuals(
    val title: String,
    val shortLabel: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val borderColor: Color
)

fun getSyncStateVisuals(state: CloudSyncState, isLoggedIn: Boolean): SyncStateVisuals {
    return when {
        !isLoggedIn -> SyncStateVisuals(
            title = "Offline (Local Storage)",
            shortLabel = "Offline",
            icon = Icons.Default.CloudQueue,
            primaryColor = Color(0xFFD0BCFF),
            secondaryColor = Color(0xFF938F99),
            backgroundColor = Color(0xFF2B2930),
            borderColor = Color(0xFF49454F)
        )
        state == CloudSyncState.SYNCED -> SyncStateVisuals(
            title = "Synced with Firestore",
            shortLabel = "Synced",
            icon = Icons.Default.CloudDone,
            primaryColor = Color(0xFF10B981),
            secondaryColor = Color(0xFF6EE7B7),
            backgroundColor = Color(0xFF064E3B).copy(alpha = 0.35f),
            borderColor = Color(0xFF10B981).copy(alpha = 0.5f)
        )
        state == CloudSyncState.SYNCING -> SyncStateVisuals(
            title = "Syncing with Firestore...",
            shortLabel = "Syncing",
            icon = Icons.Default.CloudSync,
            primaryColor = Color(0xFF818CF8),
            secondaryColor = Color(0xFFC7D2FE),
            backgroundColor = Color(0xFF312E81).copy(alpha = 0.35f),
            borderColor = Color(0xFF818CF8).copy(alpha = 0.6f)
        )
        state == CloudSyncState.OFFLINE -> SyncStateVisuals(
            title = "Offline (Local Saved)",
            shortLabel = "Offline",
            icon = Icons.Default.CloudOff,
            primaryColor = Color(0xFFF59E0B),
            secondaryColor = Color(0xFFFDE68A),
            backgroundColor = Color(0xFF451A03).copy(alpha = 0.35f),
            borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f)
        )
        else -> SyncStateVisuals(
            title = "Ready for Sync",
            shortLabel = "Ready",
            icon = Icons.Default.CloudQueue,
            primaryColor = Color(0xFFD0BCFF),
            secondaryColor = Color(0xFFE2E8F0),
            backgroundColor = Color(0xFF2B2930),
            borderColor = Color(0xFF49454F)
        )
    }
}

/**
 * Compact Live Status Indicator Pill (Used in top bars, menus, and headers)
 */
@Composable
fun FirestoreSyncMiniPill(
    syncStatus: SyncStatus,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val visuals = getSyncStateVisuals(syncStatus.state, isLoggedIn)
    val infiniteTransition = rememberInfiniteTransition(label = "sync_pulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = visuals.backgroundColor,
        border = BorderStroke(1.dp, visuals.borderColor),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("firestore_sync_mini_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Live Status Dot or Spinning Icon
            if (syncStatus.state == CloudSyncState.SYNCING && isLoggedIn) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = visuals.primaryColor,
                    modifier = Modifier
                        .size(12.dp)
                        .rotate(spinAngle)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(visuals.primaryColor)
                        .then(
                            if (syncStatus.state == CloudSyncState.SYNCED) {
                                Modifier.alpha(pulseAlpha)
                            } else Modifier
                        )
                )
            }

            Text(
                text = visuals.shortLabel,
                color = visuals.primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Rich, High-Visibility Firestore Sync Status Card for Settings and Account dialogs.
 * Displays live Firestore state (Synced/Syncing/Offline), Last Sync relative & exact timestamp,
 * target path, and an on-demand sync trigger button.
 */
@Composable
fun FirestoreSyncStatusCard(
    userProfile: UserProfile,
    syncStatus: SyncStatus,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = getSyncStateVisuals(syncStatus.state, userProfile.isLoggedIn)
    val infiniteTransition = rememberInfiniteTransition(label = "sync_card_anim")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_pulse"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_spin"
    )

    val isCurrentlySyncing = syncStatus.state == CloudSyncState.SYNCING && userProfile.isLoggedIn

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_sync_status_card"),
        shape = RoundedCornerShape(16.dp),
        color = visuals.backgroundColor,
        border = BorderStroke(1.2.dp, visuals.borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Section Label + Live Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = visuals.icon,
                        contentDescription = null,
                        tint = visuals.primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "FIRESTORE CLOUD SYNC",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = visuals.primaryColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        )
                    )
                }

                // Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = visuals.primaryColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, visuals.primaryColor.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("sync_status_badge_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(visuals.primaryColor)
                                .then(
                                    if (syncStatus.state == CloudSyncState.SYNCED) {
                                        Modifier.alpha(pulseAlpha)
                                    } else Modifier
                                )
                        )
                        Text(
                            text = visuals.shortLabel.uppercase(Locale.ROOT),
                            color = visuals.secondaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sync Status Message Banner
            Text(
                text = if (userProfile.isLoggedIn) {
                    when (syncStatus.state) {
                        CloudSyncState.SYNCED -> "Wishlist & Library are fully synced with Firebase Firestore"
                        CloudSyncState.SYNCING -> "Sending changes to cloud database..."
                        CloudSyncState.OFFLINE -> "Offline Mode: Changes saved to local device, will sync when reconnected"
                        else -> syncStatus.message
                    }
                } else {
                    "Sign in with your email to enable real-time Firebase Firestore synchronization across all your devices."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFF8FAFC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Last Sync & Metadata Metrics Container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1B24).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Last Sync Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Last Sync:",
                                color = Color(0xFFCAC4D0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatRelativeSyncTime(syncStatus.lastSyncTimestamp),
                                color = if (syncStatus.lastSyncTimestamp > 0) Color(0xFF6EE7B7) else Color(0xFF938F99),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("last_sync_relative_text")
                            )
                            if (syncStatus.lastSyncTimestamp > 0) {
                                Text(
                                    text = formatExactSyncTime(syncStatus.lastSyncTimestamp),
                                    color = Color(0xFF938F99),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.testTag("last_sync_exact_text")
                                )
                            }
                        }
                    }

                    // Cloud Database Target Row
                    if (userProfile.isLoggedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Target Collection:",
                                    color = Color(0xFFCAC4D0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = "users/${userProfile.email.take(12)}.../games",
                                color = Color(0xFFD0BCFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: "Sync Now" Button
            if (userProfile.isLoggedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF381E72),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.7f)),
                        modifier = Modifier
                            .clickable(enabled = !isCurrentlySyncing) { onSyncNow() }
                            .testTag("settings_sync_now_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isCurrentlySyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFFD0BCFF),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = if (isCurrentlySyncing) "Syncing..." else "Sync Now",
                                color = Color(0xFFD0BCFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
