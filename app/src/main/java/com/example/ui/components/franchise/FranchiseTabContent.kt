package com.example.ui.components.franchise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.franchise.FranchiseDetector
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.GameFranchise

/**
 * Dedicated Franchise Tab screen.
 * Displays user's library games grouped by series across platforms.
 */
@Composable
fun FranchiseTabContent(
    allGames: List<Game>,
    innerPadding: PaddingValues,
    selectedFranchiseName: String?,
    onSelectFranchise: (String?) -> Unit,
    onSelectGame: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamically compute franchises from user's current game library
    val franchises = remember(allGames) {
        FranchiseDetector.detectFranchises(allGames)
    }

    val selectedFranchise = remember(franchises, selectedFranchiseName) {
        franchises.firstOrNull { it.name == selectedFranchiseName }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF14121B))
    ) {
        if (selectedFranchise != null) {
            // Dedicated Franchise Detail Screen
            FranchiseDetailScreen(
                franchise = selectedFranchise,
                innerPadding = innerPadding,
                onBack = { onSelectFranchise(null) },
                onSelectGame = onSelectGame
            )
        } else {
            // Franchise Library Grid / Collections Overview
            FranchiseLibraryScreen(
                franchises = franchises,
                allGamesCount = allGames.size,
                innerPadding = innerPadding,
                onFranchiseClick = { franchise -> onSelectFranchise(franchise.name) }
            )
        }
    }
}

/**
 * Browsing screen showing all dynamic franchise cards.
 */
@Composable
private fun FranchiseLibraryScreen(
    franchises: List<GameFranchise>,
    allGamesCount: Int,
    innerPadding: PaddingValues,
    onFranchiseClick: (GameFranchise) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredFranchises = remember(franchises, searchQuery) {
        if (searchQuery.isBlank()) {
            franchises
        } else {
            val query = searchQuery.trim().lowercase()
            franchises.filter { f ->
                f.name.lowercase().contains(query) ||
                        f.games.any { it.title.lowercase().contains(query) } ||
                        f.platforms.any { it.displayName.lowercase().contains(query) || it.shortName.lowercase().contains(query) }
            }
        }
    }

    val totalFranchiseGames = remember(franchises) {
        franchises.sumOf { it.gameCount }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 80.dp,
            start = 14.dp,
            end = 14.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("franchise_library_grid")
    ) {
        // Header Section
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FRANCHISE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    color = Color(0xFFF8FAFC),
                                    fontSize = 17.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Browse your game series grouped across all platforms",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    if (franchises.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF261E38),
                            border = BorderStroke(1.dp, Color(0xFF493E64))
                        ) {
                            Text(
                                text = "${franchises.size} Series",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFD0BCFF),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Franchise Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search franchises (e.g. Assassin's Creed, Zelda)...",
                            fontSize = 13.sp,
                            color = Color(0xFF7E7A8E)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF9E95B8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF9E95B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF2E2A3E),
                        focusedContainerColor = Color(0xFF1B1826),
                        unfocusedContainerColor = Color(0xFF181523),
                        focusedTextColor = Color(0xFFF1F5F9),
                        unfocusedTextColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_franchise")
                )

                if (franchises.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing $totalFranchiseGames games across ${franchises.size} franchises",
                            fontSize = 11.sp,
                            color = Color(0xFF8F88A3)
                        )
                    }
                }
            }
        }

        // Empty state
        if (filteredFranchises.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyFranchiseState(
                    hasGamesInVault = allGamesCount > 0,
                    searchQuery = searchQuery,
                    onClearSearch = { searchQuery = "" }
                )
            }
        } else {
            // Franchise Cards Grid
            items(filteredFranchises, key = { "franchise_${it.name}" }) { franchise ->
                FranchiseCard(
                    franchise = franchise,
                    onClick = { onFranchiseClick(franchise) }
                )
            }
        }
    }
}

/**
 * Individual visual card representing an entire franchise series.
 */
@Composable
fun FranchiseCard(
    franchise: GameFranchise,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1828)),
        border = BorderStroke(1.dp, Color(0xFF2F2942)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("franchise_card_${franchise.name}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hero Artwork Banner with Scrim & Game Count Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .background(Color(0xFF100E19))
            ) {
                val bannerUrl = franchise.representativeBannerUrl
                if (bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = bannerUrl,
                        contentDescription = franchise.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Stylized gradient background with decorative iconography
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF281E48), Color(0xFF14121F))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color(0xFF574684),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // Dark gradient overlay for typography readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color(0xFF1B1828).copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                // Top Badge: Games Count
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF381E72).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0xFF6B42B8)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    val countText = if (franchise.gameCount > franchise.uniqueGameCount) {
                        "${franchise.uniqueGameCount} games (${franchise.gameCount} entries)"
                    } else {
                        "${franchise.uniqueGameCount} ${if (franchise.uniqueGameCount == 1) "game" else "games"}"
                    }
                    Text(
                        text = countText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFFEADBFF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                    )
                }

                // Top Left: Year Range
                if (franchise.yearRange.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(0.5.dp, Color(0xFF4A4458)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = franchise.yearRange,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Card Body: Franchise Title & Platforms Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = franchise.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = Color(0xFFF1F5F9),
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cross-Platform pills preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    franchise.platforms.take(4).forEach { console ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(console.brandColorHex).copy(alpha = 0.85f),
                            border = BorderStroke(0.5.dp, Color(console.accentColorHex).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = console.shortName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (franchise.platforms.size > 4) {
                        Text(
                            text = "+${franchise.platforms.size - 4}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "View Franchise",
                        tint = Color(0xFF7E7696),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Franchise Detail Screen showing the entire series collection.
 */
@Composable
fun FranchiseDetailScreen(
    franchise: GameFranchise,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onSelectGame: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedConsoleFilter by rememberSaveable { mutableStateOf("ALL") }
    var isGroupedByPlatform by rememberSaveable { mutableStateOf(true) }

    val filteredGames = remember(franchise, selectedConsoleFilter) {
        FranchiseDetector.filterFranchiseGames(
            games = franchise.games,
            selectedConsoleId = selectedConsoleFilter
        )
    }

    val gamesByPlatform = remember(filteredGames) {
        filteredGames.groupBy { it.console }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121019))
    ) {
        // Top App Bar with Back Action
        Surface(
            color = Color(0xFF171422),
            border = BorderStroke(0.dp, Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_franchise")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Franchises",
                        tint = Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = franchise.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9),
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "FRANCHISE COLLECTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF9E95B8),
                            fontSize = 10.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2C2243),
                    border = BorderStroke(1.dp, Color(0xFF4C3E72))
                ) {
                    val detailCountText = if (franchise.gameCount > franchise.uniqueGameCount) {
                        "${franchise.uniqueGameCount} Games (${franchise.gameCount} entries)"
                    } else {
                        "${franchise.uniqueGameCount} Games"
                    }
                    Text(
                        text = detailCountText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Main Scrollable Content
        LazyColumn(
            contentPadding = PaddingValues(
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Banner Section
            item(key = "franchise_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFF0F0D16))
                ) {
                    val bannerUrl = franchise.representativeBannerUrl
                    if (bannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = bannerUrl,
                            contentDescription = franchise.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF322258), Color(0xFF141021))
                                    )
                                )
                        )
                    }

                    // Scrim gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF121019).copy(alpha = 0.4f),
                                        Color(0xFF121019)
                                    )
                                )
                            )
                    )

                    // Hero Content Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = franchise.name.uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.1.sp,
                                color = Color(0xFFFFFFFF),
                                fontSize = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${franchise.gameCount} Games",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = "•", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(
                                text = "${franchise.platformCount} Platforms",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (franchise.yearRange.isNotBlank()) {
                                Text(text = "•", color = Color(0xFF64748B), fontSize = 12.sp)
                                Text(
                                    text = franchise.yearRange,
                                    fontSize = 12.sp,
                                    color = Color(0xFFD0BCFF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Platform Filter Bar
            item(key = "platform_filters") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF9E95B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FILTER BY PLATFORM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFF9E95B8),
                                fontSize = 10.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All Platforms" filter chip
                        val isAllSelected = selectedConsoleFilter.equals("ALL", ignoreCase = true)
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { selectedConsoleFilter = "ALL" },
                            label = {
                                Text(
                                    "All Platforms (${franchise.gameCount})",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1F1C2B),
                                labelColor = Color(0xFFCBD5E1),
                                selectedContainerColor = Color(0xFF381E72),
                                selectedLabelColor = Color(0xFFD0BCFF)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = Color(0xFF332E45),
                                selectedBorderColor = Color(0xFF6B42B8)
                            ),
                            modifier = Modifier.testTag("filter_platform_all")
                        )

                        // Distinct platforms present in this franchise
                        franchise.platforms.forEach { console ->
                            val isSelected = selectedConsoleFilter.equals(console.id, ignoreCase = true)
                            val count = franchise.games.count { it.consoleId.equals(console.id, ignoreCase = true) }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedConsoleFilter = if (isSelected) "ALL" else console.id
                                },
                                label = {
                                    Text(
                                        "${console.displayName} ($count)",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF1F1C2B),
                                    labelColor = Color(0xFFCBD5E1),
                                    selectedContainerColor = Color(console.brandColorHex).copy(alpha = 0.9f),
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF332E45),
                                    selectedBorderColor = Color(console.accentColorHex)
                                ),
                                modifier = Modifier.testTag("filter_platform_${console.id}")
                            )
                        }
                    }
                }
            }

            // Games Section Header with View Toggle
            item(key = "games_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isGroupedByPlatform) "GROUPED BY PLATFORM" else "CHRONOLOGICAL ORDER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${filteredGames.size} Titles)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp
                            )
                        )
                    }

                    // Mode Toggle: By Platform vs Release Order
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1B2E))
                            .border(1.dp, Color(0xFF383448), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isGroupedByPlatform) Color(0xFF4C3E72) else Color.Transparent)
                                .clickable { isGroupedByPlatform = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "By Platform",
                                fontSize = 10.sp,
                                fontWeight = if (isGroupedByPlatform) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGroupedByPlatform) Color.White else Color(0xFF9E95B8)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isGroupedByPlatform) Color(0xFF4C3E72) else Color.Transparent)
                                .clickable { isGroupedByPlatform = false }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Release Order",
                                fontSize = 10.sp,
                                fontWeight = if (!isGroupedByPlatform) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isGroupedByPlatform) Color.White else Color(0xFF9E95B8)
                            )
                        }
                    }
                }
            }

            if (isGroupedByPlatform) {
                // Grouped strictly by platform with visual platform banners
                gamesByPlatform.forEach { (console, gamesOnConsole) ->
                    item(key = "platform_header_${console.id}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(console.brandColorHex).copy(alpha = 0.95f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = console.shortName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = console.displayName.uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 12.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color(console.brandColorHex).copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (gamesOnConsole.size == 1) "1 Game" else "${gamesOnConsole.size} Games",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    items(gamesOnConsole, key = { "franchise_platform_game_${it.id}" }) { game ->
                        FranchiseGameItem(
                            game = game,
                            onClick = { onSelectGame(game) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                        )
                    }
                }
            } else {
                // Individual Game Cards in Release Order
                items(filteredGames, key = { "franchise_game_${it.id}" }) { game ->
                    FranchiseGameItem(
                        game = game,
                        onClick = { onSelectGame(game) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean, detailed game entry card inside the franchise view.
 * Displays individual cover, title, platform, release year, TTB, emulator, and status.
 */
@Composable
fun FranchiseGameItem(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1828)),
        border = BorderStroke(1.dp, Color(0xFF2B263C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("franchise_game_${game.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game Cover Thumbnail
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF241F35),
                border = BorderStroke(1.dp, Color(0xFF38324F)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .width(62.dp)
                    .height(86.dp)
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
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(game.coverGradientStart),
                                        Color(game.coverGradientEnd)
                                    )
                                )
                            )
                    ) {
                        Text(
                            text = game.console.emoji,
                            fontSize = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Game Details Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        fontSize = 14.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Primary Badges: Platform & Release Year
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Platform Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(game.console.brandColorHex).copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, Color(game.console.accentColorHex).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "${game.console.emoji} ${game.console.shortName}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Release Year
                    if (game.releaseYear > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF262234),
                            border = BorderStroke(0.5.dp, Color(0xFF3D3852))
                        ) {
                            Text(
                                text = "${game.releaseYear}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Wishlist / Play Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(game.status.badgeColorHex).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color(game.status.badgeColorHex).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${game.status.iconEmoji} ${game.status.label}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(game.status.badgeColorHex),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Secondary Badges: TTB & Emulator Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Time to Beat (TTB)
                    if (game.hasHltbData) {
                        Text(
                            text = "⏱️ ${game.formattedMainStory} story",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34D399)
                        )
                    }

                    // Emulator info
                    val emulatorName = game.emulator.ifBlank { game.console.primaryEmulator() }
                    if (emulatorName.isNotBlank()) {
                        Text(
                            text = "🕹️ $emulatorName",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Navigation chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View Details",
                tint = Color(0xFF67617C),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Polished empty state when no franchise collections or games match.
 */
@Composable
private fun EmptyFranchiseState(
    hasGamesInVault: Boolean,
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B1828),
        border = BorderStroke(1.dp, Color(0xFF2A253C)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF2B2044),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (searchQuery.isNotBlank()) {
                Text(
                    text = "No Franchises Found",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFF1F5F9)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No franchise series matched \"$searchQuery\". Try checking the spelling or search by game title.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF381E72),
                    modifier = Modifier.clickable { onClearSearch() }
                ) {
                    Text(
                        text = "Clear Search",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            } else if (!hasGamesInVault) {
                Text(
                    text = "No Games in Collection Yet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFF1F5F9)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add games from your favorite series to your vault! As you add games, franchises like Assassin's Creed, Final Fantasy, Zelda, and Resident Evil will automatically group here across all platforms.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Text(
                    text = "No Multi-Game Franchises Yet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFF1F5F9)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Franchises appear once your library contains at least 2 different games from the same series (such as Assassin's Creed, God of War, or Resident Evil). Individual games remain accessible in your main Library.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
