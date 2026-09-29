package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.WishlistStatus
import com.example.ui.viewmodel.PlatformOption
import com.example.ui.viewmodel.RandomSelectionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RandomGameDialog(
    state: RandomSelectionState,
    onPlatformSelected: (PlatformOption) -> Unit,
    onConsoleSelected: (GameConsole?) -> Unit,
    onStatusSelected: (String) -> Unit,
    onReRoll: () -> Unit,
    onViewGameDetails: (Game) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSpecificConsoleMenuOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF211F26),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF49454F))
            )
        },
        modifier = modifier.testTag("random_game_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF381E72),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RANDOM GAME PICKER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color(0xFFF8FAFC)
                            )
                        )
                        Text(
                            text = if (state.eligibleGamesCount > 0) {
                                "${state.eligibleGamesCount} eligible ${if (state.eligibleGamesCount == 1) "game" else "games"} in pool"
                            } else {
                                "Filters eligible games before selecting"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF938F99),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_random_dialog_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFCAC4D0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Platform Selector Section
            Text(
                text = "SELECT PLATFORM / CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFD0BCFF),
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Scroll of Platform Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformOption.entries.forEach { platform ->
                    val isSelected = state.selectedPlatform == platform && state.selectedConsole == null
                    Surface(
                        modifier = Modifier
                            .testTag("random_platform_chip_${platform.id.lowercase()}")
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onPlatformSelected(platform) },
                        color = if (isSelected) Color(0xFF381E72) else Color(0xFF2B2930),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = platform.emoji, fontSize = 14.sp)
                            Text(
                                text = platform.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFEADDFF) else Color(0xFFCAC4D0),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Optional Specific Console Dropdown Trigger
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(
                        modifier = Modifier
                            .testTag("random_specific_console_btn")
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (state.selectedConsole != null) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { isSpecificConsoleMenuOpen = true },
                        color = if (state.selectedConsole != null) Color(0xFF381E72) else Color(0xFF2B2930)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.selectedConsole?.let { "${it.emoji} ${it.displayName}" } ?: "🎮 Specific Console Filter...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (state.selectedConsole != null) Color(0xFFEADDFF) else Color(0xFFCAC4D0),
                                    fontSize = 11.5.sp
                                )
                            )
                            if (state.selectedConsole != null) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { onConsoleSelected(null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear specific console",
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = isSpecificConsoleMenuOpen,
                        onDismissRequest = { isSpecificConsoleMenuOpen = false },
                        modifier = Modifier
                            .background(Color(0xFF2B2930))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Consoles", color = Color(0xFFE6E1E5)) },
                            onClick = {
                                onConsoleSelected(null)
                                isSpecificConsoleMenuOpen = false
                            }
                        )
                        GameConsole.selectableConsoles().forEach { console ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(console.emoji)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = console.displayName,
                                            color = if (state.selectedConsole == console) Color(0xFFD0BCFF) else Color(0xFFE6E1E5),
                                            fontWeight = if (state.selectedConsole == console) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    onConsoleSelected(console)
                                    isSpecificConsoleMenuOpen = false
                                },
                                modifier = Modifier.testTag("random_console_menu_item_${console.id.lowercase()}")
                            )
                        }
                    }
                }

                // Status Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.selectedStatus != "ALL") {
                        val st = WishlistStatus.fromId(state.selectedStatus)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(st.badgeColorHex).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(st.badgeColorHex).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { onStatusSelected("ALL") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "${st.iconEmoji} ${st.label}", fontSize = 11.sp, color = Color.White)
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear status filter",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Result Display / Empty State Section
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.95f)) togetherWith
                            (fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.95f))
                },
                label = "random_game_result_animation"
            ) { targetState ->
                if (targetState.resultGame != null) {
                    val game = targetState.resultGame
                    val console = game.console

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("random_game_result_card")
                            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color(console.accentColorHex))
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color(console.accentColorHex).copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                        color = Color(0xFF1E1B24),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Top Header Banner with Console Name & Brand Colors
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(console.brandColorHex),
                                                Color(console.accentColorHex).copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = console.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = console.bannerText,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.2.sp,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }

                                    // Wishlist status pill
                                    val st = game.status
                                    Surface(
                                        color = Color(st.badgeColorHex).copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${st.iconEmoji} ${st.label}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 10.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Body: Cover Art + Info
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover Art Thumbnail
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(console.accentColorHex).copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .size(width = 84.dp, height = 110.dp)
                                        .shadow(8.dp, RoundedCornerShape(10.dp))
                                ) {
                                    if (game.coverArtUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = game.coverArtUrl,
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
                                                        listOf(
                                                            Color(game.coverGradientStart),
                                                            Color(game.coverGradientEnd)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(4.dp)
                                            ) {
                                                Text(text = console.emoji, fontSize = 26.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = game.title,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center
                                                    ),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                // Game Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFF8FAFC),
                                            fontSize = 16.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${game.releaseYear} • ${game.genre}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFCAC4D0),
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Rating & Emulator tags
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = String.format("%.1f", game.userRating),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFDE68A)
                                                )
                                            }
                                        }

                                        if (game.emulator.isNotBlank()) {
                                            Surface(
                                                color = Color(0xFF381E72),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = "⚡ ${game.emulator}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFD0BCFF),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty Results Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("random_game_empty_card")
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp)),
                        color = Color(0xFF2B2930),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF381E72).copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = targetState.emptyStateMessage ?: "No games found for this platform.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFE6E1E5),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.testTag("random_empty_state_text")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (targetState.selectedPlatform != PlatformOption.ALL || targetState.selectedConsole != null || targetState.selectedStatus != "ALL") {
                                Button(
                                    onClick = {
                                        onPlatformSelected(PlatformOption.ALL)
                                        onStatusSelected("ALL")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD0BCFF),
                                        contentColor = Color(0xFF381E72)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_switch_all_platforms_random")
                                ) {
                                    Text("Switch to All Platforms", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons Row: [🎲 Pick Another / Roll Again] [View Full Details]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onReRoll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_reroll_random_game")
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.resultGame != null) "Roll Again" else "Pick Random Game",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.5.sp
                    )
                }

                if (state.resultGame != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onViewGameDetails(state.resultGame)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD0BCFF)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_view_random_game_details")
                    ) {
                        Text(
                            text = "View Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
