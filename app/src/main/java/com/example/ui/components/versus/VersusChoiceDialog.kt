package com.example.ui.components.versus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.GameConsole
import com.example.data.model.WishlistStatus
import com.example.data.versus.GameVersusAnalysis
import com.example.data.versus.StressLevel
import com.example.data.versus.VersusResult
import com.example.ui.viewmodel.PlatformOption
import com.example.ui.viewmodel.VersusSelectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersusChoiceDialog(
    state: VersusSelectionState,
    allGames: List<Game>,
    onToggleGame: (Game) -> Unit,
    onRemoveGame: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConsoleFilterChange: (GameConsole) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onRunComparison: () -> Unit,
    onResetComparison: () -> Unit,
    onViewGameDetails: (Game) -> Unit,
    onSetStatusToPlaying: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectedGames = remember(state.selectedGameIds, allGames) {
        state.selectedGameIds.mapNotNull { id -> allGames.find { it.id == id } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF19171F),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF534F5E))
            )
        },
        modifier = modifier.testTag("versus_choice_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding()
        ) {
            // Header Bar
            VersusHeader(
                selectedCount = selectedGames.size,
                hasResult = state.result != null,
                onDismiss = onDismiss
            )

            HorizontalDivider(
                color = Color(0xFF2C2836),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Animated Screen Content (Selection View vs Comparison View)
            AnimatedContent(
                targetState = state.result != null,
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                },
                label = "versus_flow_transition",
                modifier = Modifier.weight(1f)
            ) { hasResult ->
                if (hasResult && state.result != null) {
                    VersusResultScreen(
                        result = state.result,
                        onResetComparison = onResetComparison,
                        onViewGameDetails = onViewGameDetails,
                        onSetStatusToPlaying = onSetStatusToPlaying
                    )
                } else {
                    VersusSelectionScreen(
                        selectedGames = selectedGames,
                        allGames = allGames,
                        isAnalyzing = state.isAnalyzing,
                        errorMessage = state.errorMessage,
                        searchQuery = state.searchQuery,
                        filterConsole = state.filterConsole,
                        filterStatus = state.filterStatus,
                        onToggleGame = onToggleGame,
                        onRemoveGame = onRemoveGame,
                        onClearSelection = onClearSelection,
                        onSearchQueryChange = onSearchQueryChange,
                        onConsoleFilterChange = onConsoleFilterChange,
                        onStatusFilterChange = onStatusFilterChange,
                        onRunComparison = onRunComparison
                    )
                }
            }
        }
    }
}

@Composable
private fun VersusHeader(
    selectedCount: Int,
    hasResult: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF381E72),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.6f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⚔️",
                        fontSize = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VERSUS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFD0BCFF).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "CHOICE PICKER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0BCFF),
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = if (hasResult) "Decisive comparison & recommendation" else "Select 2 or 3 games to compare",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                )
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .testTag("versus_close_btn")
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF272430))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFFCAC4D0),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ==========================================
// 1️⃣ SELECTION SCREEN
// ==========================================
@Composable
private fun VersusSelectionScreen(
    selectedGames: List<Game>,
    allGames: List<Game>,
    isAnalyzing: Boolean,
    errorMessage: String?,
    searchQuery: String,
    filterConsole: GameConsole,
    filterStatus: String,
    onToggleGame: (Game) -> Unit,
    onRemoveGame: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConsoleFilterChange: (GameConsole) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onRunComparison: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Selected Games Dock (Slots 1, 2, 3)
        VersusSelectionDock(
            selectedGames = selectedGames,
            onRemoveGame = onRemoveGame,
            onClearSelection = onClearSelection
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Compare Action Button
        Button(
            onClick = onRunComparison,
            enabled = selectedGames.size in 2..3 && !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("versus_compare_action_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF2C2836),
                disabledContentColor = Color(0xFF64748B)
            )
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Analyzing Vibe, Time & Stress...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = if (selectedGames.size in 2..3) Color(0xFFFBBF24) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (selectedGames.size) {
                        0 -> "Select 2 or 3 Games Below"
                        1 -> "Select 1 More Game to Compare"
                        2 -> "⚔️ Compare 2 Games & Pick Winner"
                        else -> "⚔️ Compare 3 Games & Pick Winner"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Error message banner
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF451A1A),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFFCA5A5),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search and Filter Tools
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("versus_search_input"),
            placeholder = {
                Text(
                    "Search games in your library...",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFFCAC4D0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF23202C),
                unfocusedContainerColor = Color(0xFF23202C),
                focusedBorderColor = Color(0xFFD0BCFF),
                unfocusedBorderColor = Color(0xFF383444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Platform Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val consoles = listOf(GameConsole.ALL) + GameConsole.selectableConsoles()
            consoles.forEach { console ->
                val isSelected = filterConsole == console
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(console.brandColorHex).copy(alpha = 0.9f) else Color(0xFF23202C),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(console.accentColorHex) else Color(0xFF383444)
                    ),
                    modifier = Modifier
                        .clickable { onConsoleFilterChange(console) }
                        .testTag("versus_filter_console_${console.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = console.emoji, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = console.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filtered Games List
        val filteredGames = remember(allGames, searchQuery, filterConsole, filterStatus) {
            allGames.filter { game ->
                val matchesSearch = searchQuery.isBlank() ||
                        game.title.contains(searchQuery, ignoreCase = true) ||
                        game.genre.contains(searchQuery, ignoreCase = true)
                val matchesConsole = filterConsole == GameConsole.ALL ||
                        game.consoleId.equals(filterConsole.id, ignoreCase = true)
                val matchesStatus = filterStatus == "ALL" ||
                        game.wishlistStatus.equals(filterStatus, ignoreCase = true)

                matchesSearch && matchesConsole && matchesStatus
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredGames.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎮",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (allGames.isEmpty()) "No games in library" else "No matching games found",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Try adjusting your search or console filters.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            } else {
                items(filteredGames, key = { it.id }) { game ->
                    val isSelected = selectedGames.any { it.id == game.id }
                    VersusGameSelectableItem(
                        game = game,
                        isSelected = isSelected,
                        onToggle = { onToggleGame(game) }
                    )
                }
            }
        }
    }
}

// ==========================================
// SELECTED GAMES DOCK (3 SLOTS)
// ==========================================
@Composable
private fun VersusSelectionDock(
    selectedGames: List<Game>,
    onRemoveGame: (Long) -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF221F2C),
        border = BorderStroke(1.dp, Color(0xFF383444))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SELECTED CANDIDATES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFFD0BCFF),
                            fontSize = 10.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (selectedGames.size) {
                            0 -> Color(0xFF332D41)
                            1 -> Color(0xFF451A1A)
                            2 -> Color(0xFF1E3A8A)
                            else -> Color(0xFF065F46)
                        }
                    ) {
                        Text(
                            text = "${selectedGames.size}/3",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = when (selectedGames.size) {
                                    0 -> Color(0xFF94A3B8)
                                    1 -> Color(0xFFFCA5A5)
                                    2 -> Color(0xFF93C5FD)
                                    else -> Color(0xFF6EE7B7)
                                },
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (selectedGames.isNotEmpty()) {
                    TextButton(
                        onClick = onClearSelection,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (slotIndex in 0..2) {
                    val game = selectedGames.getOrNull(slotIndex)
                    Box(modifier = Modifier.weight(1f)) {
                        if (game != null) {
                            SelectedGameSlotCard(
                                game = game,
                                onRemove = { onRemoveGame(game.id) }
                            )
                        } else {
                            EmptyGameSlotCard(slotNumber = slotIndex + 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedGameSlotCard(
    game: Game,
    onRemove: () -> Unit
) {
    val console = game.console
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF2A2636),
        border = BorderStroke(1.dp, Color(console.accentColorHex).copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Game Cover / Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(game.coverGradientStart),
                                    Color(game.coverGradientEnd)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (game.coverArtUrl.isNotBlank()) {
                        AsyncImage(
                            model = game.coverArtUrl,
                            contentDescription = game.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = console.emoji,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                // Console Pill
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(console.brandColorHex).copy(alpha = 0.85f)
                ) {
                    Text(
                        text = console.shortName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1B26))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove game",
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyGameSlotCard(slotNumber: Int) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E1B26),
        border = BorderStroke(1.dp, Color(0xFF383444)),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF64748B)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Slot $slotNumber",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            )
            Text(
                text = "Select game",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF475569),
                    fontSize = 9.sp
                )
            )
        }
    }
}

// ==========================================
// SELECTABLE GAME ROW ITEM
// ==========================================
@Composable
private fun VersusGameSelectableItem(
    game: Game,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val console = game.console
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF2C243B) else Color(0xFF201D28),
        border = BorderStroke(
            1.2.dp,
            if (isSelected) Color(0xFFD0BCFF) else Color(0xFF332F3D)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("versus_item_${game.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Art Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(game.coverGradientStart),
                                Color(game.coverGradientEnd)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (game.coverArtUrl.isNotBlank()) {
                    AsyncImage(
                        model = game.coverArtUrl,
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = console.emoji, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Game Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Console Pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(console.brandColorHex).copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = console.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    if (game.genre.isNotBlank()) {
                        Text(
                            text = "• ${game.genre}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (game.hltbMainStoryHours > 0f) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "⏱️ ${game.formattedMainStory}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF38BDF8),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Selection Checkbox / Indicator
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color(0xFF7C3AED) else Color(0xFF2A2735),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFFD0BCFF) else Color(0xFF474354)
                ),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2️⃣ VERSUS RESULT & RECOMMENDATION SCREEN
// ==========================================
@Composable
private fun VersusResultScreen(
    result: VersusResult,
    onResetComparison: () -> Unit,
    onViewGameDetails: (Game) -> Unit,
    onSetStatusToPlaying: (Long) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // 🏆 MY PICK: FIRM RECOMMENDATION HERO CARD
        VersusWinnerHighlightCard(
            winnerGame = result.winnerGame,
            reasoningSummary = result.reasoningSummary,
            primaryAdvantage = result.primaryAdvantage,
            isAiGenerated = result.isAiGenerated,
            onViewGameDetails = { onViewGameDetails(result.winnerGame) },
            onSetStatusToPlaying = { onSetStatusToPlaying(result.winnerGame.id) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Side-by-Side Candidates Header
        Text(
            text = "⚡ SIDE-BY-SIDE MATCHUP",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color(0xFFD0BCFF),
                fontSize = 11.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Candidate Cards Row (2 or 3 columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            result.games.forEach { analysis ->
                val isWinner = analysis.game.id == result.winnerGame.id
                Box(modifier = Modifier.weight(1f)) {
                    VersusCandidateCard(
                        analysis = analysis,
                        isWinner = isWinner,
                        onClick = { onViewGameDetails(analysis.game) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CRITERIA COMPARISON MATRIX TABLE
        VersusComparisonMatrix(result = result)

        Spacer(modifier = Modifier.height(20.dp))

        // Reset / Compare Others Action Button
        OutlinedButton(
            onClick = onResetComparison,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("versus_reset_selection_btn"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFD0BCFF)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Compare Different Games",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
// 🏆 WINNER HERO CARD (DECISIVE PICK)
// ==========================================
@Composable
private fun VersusWinnerHighlightCard(
    winnerGame: Game,
    reasoningSummary: String,
    primaryAdvantage: String,
    isAiGenerated: Boolean,
    onViewGameDetails: () -> Unit,
    onSetStatusToPlaying: () -> Unit
) {
    val console = winnerGame.console

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFF59E0B)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF261D15),
        border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFFCD34D))))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF59E0B),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MY PICK — DECISIVE WINNER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E1408),
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                if (isAiGenerated) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF381E72).copy(alpha = 0.8f),
                        border = BorderStroke(0.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✨", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Gemini AI Evaluated",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFE9D5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Game Hero Header (Cover + Title + Console)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(winnerGame.coverGradientStart),
                                    Color(winnerGame.coverGradientEnd)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (winnerGame.coverArtUrl.isNotBlank()) {
                        AsyncImage(
                            model = winnerGame.coverArtUrl,
                            contentDescription = winnerGame.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = console.emoji, fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = winnerGame.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFFBEB),
                            fontSize = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(console.brandColorHex).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = console.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 9.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (primaryAdvantage.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF78350F).copy(alpha = 0.6f),
                                border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = primaryAdvantage,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFFDE68A),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Firm 1-3 Sentence Reasoning Verdict
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1C140C),
                border = BorderStroke(1.dp, Color(0xFFB45309).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = reasoningSummary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFFEF3C7),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick CTAs: View Details or Set to Playing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSetStatusToPlaying,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(40.dp)
                        .testTag("versus_play_winner_btn"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play This Game",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = onViewGameDetails,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("versus_details_winner_btn"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFDE68A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View TTB",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// CANDIDATE CARD IN MATCHUP ROW
// ==========================================
@Composable
private fun VersusCandidateCard(
    analysis: GameVersusAnalysis,
    isWinner: Boolean,
    onClick: () -> Unit
) {
    val game = analysis.game
    val console = game.console

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isWinner) Color(0xFF281E15) else Color(0xFF201D28),
        border = BorderStroke(
            if (isWinner) 1.5.dp else 1.dp,
            if (isWinner) Color(0xFFF59E0B) else Color(0xFF332F3D)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isWinner) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "👑 WINNER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E1408),
                            fontSize = 8.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(game.coverGradientStart),
                                Color(game.coverGradientEnd)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (game.coverArtUrl.isNotBlank()) {
                    AsyncImage(
                        model = game.coverArtUrl,
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = console.emoji, fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = game.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(console.brandColorHex).copy(alpha = 0.85f)
            ) {
                Text(
                    text = console.shortName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 8.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

// ==========================================
// CRITERIA COMPARISON MATRIX TABLE
// ==========================================
@Composable
private fun VersusComparisonMatrix(result: VersusResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF221F2C),
        border = BorderStroke(1.dp, Color(0xFF383444))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "CRITERIA COMPARISON MATRIX",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFFD0BCFF),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1️⃣ ATMOSPHERE / VIBE ROW
            MatrixCriterionBlock(
                title = "🎭 ATMOSPHERE & VIBE",
                subtitle = "Mood, sensory feeling & experience type",
                accentColor = Color(0xFFC084FC)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    result.games.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2B2538),
                                border = BorderStroke(1.dp, Color(0xFF4C3D61)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = item.game.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.vibeIcon} ${item.vibe}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF3E8FF),
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2F2A3B), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // 2️⃣ TIME REQUIRED ROW
            MatrixCriterionBlock(
                title = "⏱️ TIME REQUIRED",
                subtitle = "Main story / campaign duration",
                accentColor = Color(0xFF38BDF8)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    result.games.forEach { item ->
                        val isUnknown = item.timeRequired.equals("Unknown", ignoreCase = true)
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E2836),
                                border = BorderStroke(1.dp, Color(0xFF254B6E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = item.game.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isUnknown) "Unknown" else item.timeRequired,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isUnknown) Color(0xFF94A3B8) else Color(0xFF7DD3FC),
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2F2A3B), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // 3️⃣ DIFFICULTY / STRESS ROW
            MatrixCriterionBlock(
                title = "💀 DIFFICULTY & STRESS",
                subtitle = "Mental friction, challenge & penalty severity",
                accentColor = Color(0xFFF87171)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    result.games.forEach { item ->
                        val stressColor = Color(item.stressLevel.colorHex)
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = stressColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, stressColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = item.game.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.stressLevel.emoji} ${item.stressLevel.label}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = stressColor,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixCriterionBlock(
    title: String,
    subtitle: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}
