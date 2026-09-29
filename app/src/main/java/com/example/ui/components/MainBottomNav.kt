package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.auth.UserProfile
import com.example.data.sync.SyncStatus
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.ShelfViewMode
import com.example.data.model.WishlistStatus
import com.example.ui.components.shelf.DigitalShelfContainer
import com.example.ui.components.shelf.ShelfViewSelectorDropdown
import com.example.ui.screens.ActiveFilterTag
import com.example.ui.screens.EmptyWishlistState
import com.example.ui.viewmodel.SortOption

enum class MainBottomNavTab(val label: String, val testTag: String) {
    LIBRARY("Library", "bottom_nav_library"),
    PLATFORMS("Platforms", "bottom_nav_platforms"),
    FRANCHISE("Franchise", "bottom_nav_franchise"),
    ACCOUNT("Account", "bottom_nav_account")
}

@Composable
fun MainBottomNavBar(
    currentTab: MainBottomNavTab,
    onTabSelected: (MainBottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = Color(0xFF13111A),
        contentColor = Color(0xFFD0BCFF),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, Color(0xFF262232)),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("main_bottom_nav_bar")
    ) {
        MainBottomNavTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            MainBottomNavTab.LIBRARY -> Icons.Default.VideoLibrary
                            MainBottomNavTab.PLATFORMS -> Icons.Default.SportsEsports
                            MainBottomNavTab.FRANCHISE -> Icons.Default.Layers
                            MainBottomNavTab.ACCOUNT -> Icons.Default.Person
                        },
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF381E72),
                    selectedTextColor = Color(0xFFD0BCFF),
                    indicatorColor = Color(0xFFD0BCFF),
                    unselectedIconColor = Color(0xFF8C869E),
                    unselectedTextColor = Color(0xFF8C869E)
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

@Composable
fun LibraryTabContent(
    games: List<Game>,
    allGames: List<Game>,
    searchQuery: String,
    selectedConsole: GameConsole,
    selectedStatus: String,
    sortBy: String,
    shelfViewMode: ShelfViewMode,
    activeFilterCount: Int,
    consoleCounts: Map<String, Int>,
    isAiDetecting: Boolean,
    aiDetectionStatus: String?,
    innerPadding: PaddingValues,
    onSearchQueryChange: (String) -> Unit,
    onConsoleSelected: (GameConsole) -> Unit,
    onStatusSelected: (String) -> Unit,
    onSortBySelected: (String) -> Unit,
    onSetShelfViewMode: (ShelfViewMode) -> Unit,
    onResetAllFilters: () -> Unit,
    onOpenAddGameDialog: () -> Unit,
    onSelectGame: (Game) -> Unit,
    onTrailerClick: (Game) -> Unit,
    onDeleteClick: (Game) -> Unit,
    onAddWithAi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding() + 4.dp,
                start = 12.dp,
                end = 12.dp
            )
    ) {
        // Compact Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E1B4B),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.6f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = "App Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "THE TAVERN",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp,
                        color = Color(0xFFF8FAFC),
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFF2B2930),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF381E72))
                ) {
                    Text(
                        text = "${games.size}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Quick Actions: Search, Filter, Sort, View mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier
                        .testTag("toggle_search_btn")
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSearchExpanded || searchQuery.isNotBlank()) Color(0xFF381E72) else Color(0xFF262232))
                        .border(1.dp, if (isSearchExpanded || searchQuery.isNotBlank()) Color(0xFFD0BCFF) else Color(0xFF3F3B4D), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search games",
                        tint = if (isSearchExpanded || searchQuery.isNotBlank()) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { isSortMenuOpen = true },
                        modifier = Modifier
                            .testTag("sort_menu_btn")
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262232))
                            .border(1.dp, Color(0xFF3F3B4D), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isSortMenuOpen,
                        onDismissRequest = { isSortMenuOpen = false },
                        modifier = Modifier
                            .background(Color(0xFF2B2930))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (sortBy == option.id) Color(0xFFD0BCFF) else Color(0xFFE6E1E5),
                                        fontWeight = if (sortBy == option.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortBySelected(option.id)
                                    isSortMenuOpen = false
                                },
                                modifier = Modifier.testTag("sort_option_${option.id}")
                            )
                        }
                    }
                }

                ShelfViewSelectorDropdown(
                    currentMode = shelfViewMode,
                    onModeSelected = onSetShelfViewMode
                )
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(
            visible = isSearchExpanded || searchQuery.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = if (selectedConsole != GameConsole.ALL) "Search in ${selectedConsole.displayName}..." else "Search all games by title...",
                        fontSize = 12.5.sp,
                        color = Color(0xFF938F99),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            modifier = Modifier.testTag("clear_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color(0xFFCAC4D0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("search_games_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF3F3B4D),
                    focusedContainerColor = Color(0xFF1F1D28),
                    unfocusedContainerColor = Color(0xFF1F1D28),
                    focusedTextColor = Color(0xFFE6E1E5),
                    unfocusedTextColor = Color(0xFFE6E1E5)
                )
            )
        }

        // Horizontal Scrollable Platform Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isAllSelected = selectedConsole == GameConsole.ALL
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isAllSelected) Color(0xFFD0BCFF) else Color(0xFF221F2D),
                border = BorderStroke(1.dp, if (isAllSelected) Color(0xFFD0BCFF) else Color(0xFF343042)),
                modifier = Modifier
                    .testTag("btn_back_to_consoles")
                    .clickable { onConsoleSelected(GameConsole.ALL) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🎮 All",
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllSelected) Color(0xFF381E72) else Color(0xFFE2E8F0)
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (isAllSelected) Color(0xFF381E72) else Color(0xFF343042)
                    ) {
                        Text(
                            text = "${allGames.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllSelected) Color(0xFFD0BCFF) else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            GameConsole.selectableConsoles().forEach { console ->
                val isSelected = selectedConsole == console
                val count = consoleCounts[console.id] ?: 0
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Color(console.accentColorHex).copy(alpha = 0.25f) else Color(0xFF221F2D),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(console.accentColorHex) else Color(0xFF343042)
                    ),
                    modifier = Modifier.clickable { onConsoleSelected(console) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${console.emoji} ${console.shortName}",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                        )
                        if (count > 0) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color(console.accentColorHex) else Color(0xFF343042)
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Filters Tags
        AnimatedVisibility(
            visible = activeFilterCount > 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searchQuery.isNotBlank()) {
                    ActiveFilterTag(
                        label = "\"$searchQuery\"",
                        onRemove = { onSearchQueryChange("") },
                        testTag = "active_filter_query"
                    )
                }
                if (selectedConsole != GameConsole.ALL) {
                    ActiveFilterTag(
                        label = "${selectedConsole.emoji} ${selectedConsole.shortName}",
                        onRemove = { onConsoleSelected(GameConsole.ALL) },
                        testTag = "active_filter_console"
                    )
                }
                if (selectedStatus != "ALL") {
                    val st = WishlistStatus.fromId(selectedStatus)
                    ActiveFilterTag(
                        label = "${st.iconEmoji} ${st.label}",
                        onRemove = { onStatusSelected("ALL") },
                        testTag = "active_filter_status"
                    )
                }
                Surface(
                    modifier = Modifier
                        .testTag("active_filters_clear_all_btn")
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFEFB8C8).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onResetAllFilters() },
                    color = Color(0xFF492532),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFEFB8C8)
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Content
        if (games.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                EmptyWishlistState(
                    searchQuery = searchQuery,
                    selectedConsole = selectedConsole,
                    selectedStatus = selectedStatus,
                    activeFilterCount = activeFilterCount,
                    isDetecting = isAiDetecting,
                    statusMessage = aiDetectionStatus,
                    onResetFilters = onResetAllFilters,
                    onAddWithAi = onAddWithAi,
                    onAddGame = onOpenAddGameDialog
                )
            }
        } else {
            DigitalShelfContainer(
                games = games,
                currentMode = shelfViewMode,
                onSelectGame = onSelectGame,
                onTrailerClick = onTrailerClick,
                onDeleteClick = onDeleteClick,
                contentPadding = PaddingValues(
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PlatformsTabContent(
    consoleCategories: List<Triple<String, String, List<GameConsole>>>,
    consoleCounts: Map<String, Int>,
    selectedConsole: GameConsole,
    innerPadding: PaddingValues,
    onConsoleSelected: (GameConsole) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 4.dp,
            bottom = innerPadding.calculateBottomPadding() + 80.dp,
            start = 12.dp,
            end = 12.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = "PLATFORMS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFFF8FAFC),
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = "Select a console to browse games in your collection",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                )
            }
        }

        consoleCategories.forEach { (catTitle, catIcon, consoles) ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "header_$catTitle") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$catIcon $catTitle",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color(0xFF332F42))
                    )
                }
            }

            items(consoles, key = { "console_${it.id}" }) { console ->
                val count = consoleCounts[console.id] ?: 0
                val isSelected = selectedConsole == console

                ConsoleCard(
                    console = console,
                    gameCount = count,
                    isSelected = isSelected,
                    onClick = { onConsoleSelected(console) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DiscoverTabContent(
    allGames: List<Game>,
    innerPadding: PaddingValues,
    onOpenRandomGame: () -> Unit,
    onOpenVersusPicker: () -> Unit,
    onSelectGame: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 14.dp,
                end = 14.dp
            )
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "DISCOVER",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = Color(0xFFF8FAFC),
                fontSize = 16.sp
            )
        )
        Text(
            text = "Find your next game with random picks and head-to-head match-ups",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Random Game Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E1B2E),
            border = BorderStroke(1.dp, Color(0xFF381E72)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_random_game")
                .clickable { onOpenRandomGame() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF381E72),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Roll Random Game",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Roll Random Game",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = "Can't decide what to play? Let fate choose a title from your library.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Versus Picker Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF261828),
            border = BorderStroke(1.dp, Color(0xFF6B21A8).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_versus_picker")
                .clickable { onOpenVersusPicker() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4A154B),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "⚔️", fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Versus Picker",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = "Face off two games head-to-head to pick your priority queue.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        val wantToPlayGames = remember(allGames) {
            allGames.filter { it.wishlistStatus == WishlistStatus.WANT_TO_PLAY.id }.take(6)
        }
        if (wantToPlayGames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "✨ Want to Play (${wantToPlayGames.size})",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0),
                fontSize = 13.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                wantToPlayGames.forEach { g ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1B2E),
                        border = BorderStroke(1.dp, Color(0xFF332F42)),
                        modifier = Modifier
                            .width(90.dp)
                            .clickable { onSelectGame(g) }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(125.dp)
                                    .background(Color(0xFF262232))
                            ) {
                                if (g.coverArtUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = g.coverArtUrl,
                                        contentDescription = g.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Text(
                                text = g.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountTabContent(
    allGames: List<Game>,
    userProfile: UserProfile,
    syncStatus: SyncStatus,
    innerPadding: PaddingValues,
    onOpenAccountDialog: () -> Unit,
    onOpenBackupSheet: () -> Unit,
    onOpenRandomGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 14.dp,
                end = 14.dp
            )
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ACCOUNT & SETTINGS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = Color(0xFFF8FAFC),
                fontSize = 16.sp
            )
        )
        Text(
            text = "Cloud synchronization, data backup, and collection statistics",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Profile Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E1B2E),
            border = BorderStroke(1.dp, Color(0xFF332F42)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(userProfile.avatarColorHex),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (userProfile.isLoggedIn) userProfile.initials else "👤",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = userProfile.displayHandle,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9),
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (userProfile.isLoggedIn) userProfile.email else "Local Offline Vault",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (userProfile.isLoggedIn) Color(0xFF2B2930) else Color(0xFF381E72),
                    border = BorderStroke(1.dp, if (userProfile.isLoggedIn) Color(0xFF49454F) else Color(0xFFD0BCFF)),
                    modifier = Modifier
                        .testTag("user_account_header_btn")
                        .clickable { onOpenAccountDialog() }
                ) {
                    Text(
                        text = if (userProfile.isLoggedIn) "Account" else "Sign In",
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cloud Sync Status Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E1B2E),
            border = BorderStroke(1.dp, Color(0xFF332F42)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firebase Cloud Sync",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9),
                            fontSize = 13.5.sp
                        )
                    }
                    FirestoreSyncMiniPill(
                        syncStatus = syncStatus,
                        isLoggedIn = userProfile.isLoggedIn
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (userProfile.isLoggedIn) "Automatic real-time sync enabled for your cloud library." else "Sign in to back up and synchronize your games across devices.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Backup & Export Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E1B2E),
            border = BorderStroke(1.dp, Color(0xFF332F42)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("backup_library_header_btn")
                .clickable { onOpenBackupSheet() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Export & Restore Library",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = "Download JSON/CSV backups or restore your previous library collection.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Library Statistics
        Text(
            text = "Collection Stats",
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE2E8F0),
            fontSize = 13.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1B2E),
                border = BorderStroke(1.dp, Color(0xFF332F42)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "${allGames.size}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFD0BCFF))
                    Text(text = "Total Games", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1B2E),
                border = BorderStroke(1.dp, Color(0xFF332F42)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val playingCount = allGames.count { it.wishlistStatus == WishlistStatus.PLAYING.id }
                    Text(text = "$playingCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
                    Text(text = "Playing", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1B2E),
                border = BorderStroke(1.dp, Color(0xFF332F42)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val completedCount = allGames.count { it.wishlistStatus == WishlistStatus.COMPLETED.id }
                    Text(text = "$completedCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFA78BFA))
                    Text(text = "Completed", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Random Game Picker Section (Positioned below Collection Stats)
        Text(
            text = "Game Discovery",
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE2E8F0),
            fontSize = 13.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E1B2E),
            border = BorderStroke(1.dp, Color(0xFF381E72)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_random_game_account")
                .clickable { onOpenRandomGame() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF381E72),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Roll Random Game",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Roll Random Game",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Can't decide what to play? Let fate choose a title from your library.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}
