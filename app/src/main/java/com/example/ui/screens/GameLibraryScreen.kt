package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.WishlistStatus
import com.example.data.model.ShelfViewMode
import com.example.ui.components.AddEditGameDialog
import com.example.ui.components.ConsoleCard
import com.example.ui.components.ConsoleGameListHeader
import com.example.ui.components.DiscCoverCard
import com.example.ui.components.ExportBackupSheet
import com.example.ui.components.GameDetailSheet
import com.example.ui.components.RandomGameDialog
import com.example.ui.components.UserAccountDialog
import com.example.ui.components.YouTubeTrailerDialog
import com.example.ui.components.versus.VersusChoiceDialog
import com.example.ui.components.shelf.DigitalShelfContainer
import com.example.ui.components.shelf.ShelfViewSelectorBar
import com.example.ui.components.shelf.ShelfViewSelectorDropdown
import com.example.ui.components.MainBottomNavTab
import com.example.ui.components.MainBottomNavBar
import com.example.ui.components.LibraryTabContent
import com.example.ui.components.PlatformsTabContent
import com.example.ui.components.DiscoverTabContent
import com.example.ui.components.franchise.FranchiseTabContent
import com.example.ui.components.AccountTabContent
import com.example.ui.viewmodel.GameVaultViewModel
import com.example.ui.viewmodel.SortOption

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    viewModel: GameVaultViewModel,
    modifier: Modifier = Modifier
) {
    val games by viewModel.filteredGames.collectAsStateWithLifecycle()
    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedConsole by viewModel.selectedConsole.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedYearEra by viewModel.selectedYearEra.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val consoleCounts by viewModel.consoleCounts.collectAsStateWithLifecycle()
    val activeFilterCount by viewModel.activeFilterCount.collectAsStateWithLifecycle()
    val libraryStats by viewModel.libraryStats.collectAsStateWithLifecycle()
    val shelfViewMode by viewModel.shelfViewMode.collectAsStateWithLifecycle()

    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val gameToEdit by viewModel.gameToEdit.collectAsStateWithLifecycle()
    val isAddDialogOpen by viewModel.isAddDialogOpen.collectAsStateWithLifecycle()
    val isAiDetecting by viewModel.isAiDetecting.collectAsStateWithLifecycle()
    val aiDetectionStatus by viewModel.aiDetectionStatus.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isAccountDialogOpen by viewModel.isAccountDialogOpen.collectAsStateWithLifecycle()
    val isBackupSheetOpen by viewModel.isBackupSheetOpen.collectAsStateWithLifecycle()
    val backupOperationState by viewModel.backupOperationState.collectAsStateWithLifecycle()
    val randomSelectionState by viewModel.randomSelectionState.collectAsStateWithLifecycle()
    val versusState by viewModel.versusState.collectAsStateWithLifecycle()
    val hltbLoadingStates by viewModel.hltbLoadingStates.collectAsStateWithLifecycle()
    val isRefreshingMetadata by viewModel.isRefreshingMetadata.collectAsStateWithLifecycle()

    var isGridView by remember { mutableStateOf(true) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var currentNavTab by remember { mutableStateOf(MainBottomNavTab.LIBRARY) }
    var selectedFranchiseName by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var quickGameName by remember { mutableStateOf("") }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }
    var trailerGame by remember { mutableStateOf<Game?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Predictively & cleanly handle Back navigation and IME dismissal
    val hasBackAction = isAddDialogOpen || gameToEdit != null || 
            selectedGame != null || isAccountDialogOpen || isBackupSheetOpen || randomSelectionState.isOpen || 
            versusState.isOpen || trailerGame != null || gameToDelete != null || selectedFranchiseName != null || currentNavTab != MainBottomNavTab.LIBRARY || searchQuery.isNotBlank() || selectedConsole != GameConsole.ALL

    BackHandler(enabled = hasBackAction) {
        focusManager.clearFocus()
        keyboardController?.hide()
        when {
            trailerGame != null -> trailerGame = null
            gameToDelete != null -> gameToDelete = null
            selectedGame != null -> viewModel.onSelectGame(null)
            isAddDialogOpen || gameToEdit != null -> viewModel.closeDialogs()
            isAccountDialogOpen -> viewModel.closeAccountDialog()
            isBackupSheetOpen -> viewModel.closeBackupSheet()
            versusState.isOpen -> viewModel.closeVersusPicker()
            randomSelectionState.isOpen -> viewModel.closeRandomGameDialog()
            searchQuery.isNotBlank() -> viewModel.onSearchQueryChange("")
            selectedConsole != GameConsole.ALL -> viewModel.onConsoleSelected(GameConsole.ALL)
            selectedFranchiseName != null -> selectedFranchiseName = null
            currentNavTab != MainBottomNavTab.LIBRARY -> currentNavTab = MainBottomNavTab.LIBRARY
        }
    }

    val consoleCategories = remember {
        listOf(
            Triple("PC & All Platforms", "💻", listOf(GameConsole.ALL, GameConsole.PC)),
            Triple("PlayStation (2000 – 2020)", "🎮", listOf(GameConsole.PS2, GameConsole.PSP, GameConsole.PS3, GameConsole.PS_VITA, GameConsole.PS4, GameConsole.PS5)),
            Triple("Nintendo (1996 – 2017)", "🕹️", listOf(GameConsole.N64, GameConsole.GBA, GameConsole.GAMECUBE, GameConsole.NINTENDO_DS, GameConsole.WII, GameConsole.NINTENDO_3DS, GameConsole.WII_U, GameConsole.SWITCH)),
            Triple("Xbox (2001 – 2013)", "💚", listOf(GameConsole.XBOX, GameConsole.XBOX_360, GameConsole.XBOX_ONE))
        )
    }

    // If search query is typed, auto-switch to library to show results
    androidx.compose.runtime.LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            currentNavTab = MainBottomNavTab.LIBRARY
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_library_screen"),
        containerColor = Color(0xFF1C1B1F),
        bottomBar = {
            MainBottomNavBar(
                currentTab = currentNavTab,
                onTabSelected = { tab ->
                    currentNavTab = tab
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            )
        },
        floatingActionButton = {
            if (currentNavTab == MainBottomNavTab.LIBRARY) {
                FloatingActionButton(
                    onClick = { viewModel.openAddGameDialog() },
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .testTag("fab_add_game")
                        .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFD0BCFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Game", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Game", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        when (currentNavTab) {
            MainBottomNavTab.LIBRARY -> {
                LibraryTabContent(
                    games = games,
                    allGames = allGames,
                    searchQuery = searchQuery,
                    selectedConsole = selectedConsole,
                    selectedStatus = selectedStatus,
                    sortBy = sortBy,
                    shelfViewMode = shelfViewMode,
                    activeFilterCount = activeFilterCount,
                    consoleCounts = consoleCounts,
                    isAiDetecting = isAiDetecting,
                    aiDetectionStatus = aiDetectionStatus,
                    innerPadding = innerPadding,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onConsoleSelected = { viewModel.onConsoleSelected(it) },
                    onStatusSelected = { viewModel.onStatusSelected(it) },
                    onSortBySelected = { viewModel.onSortBySelected(it) },
                    onSetShelfViewMode = { viewModel.setShelfViewMode(it) },
                    onResetAllFilters = { viewModel.resetAllFilters() },
                    onOpenAddGameDialog = { viewModel.openAddGameDialog() },
                    onSelectGame = { viewModel.onSelectGame(it) },
                    onTrailerClick = { trailerGame = it },
                    onDeleteClick = { gameToDelete = it },
                    onAddWithAi = { gameName ->
                        keyboardController?.hide()
                        viewModel.detectGameWithAi(gameName) { detectedGame ->
                            viewModel.addAiDetectedGame(detectedGame)
                        }
                    }
                )
            }
            MainBottomNavTab.PLATFORMS -> {
                PlatformsTabContent(
                    consoleCategories = consoleCategories,
                    consoleCounts = consoleCounts,
                    selectedConsole = selectedConsole,
                    innerPadding = innerPadding,
                    onConsoleSelected = { console ->
                        viewModel.onConsoleSelected(console)
                        currentNavTab = MainBottomNavTab.LIBRARY
                    }
                )
            }
            MainBottomNavTab.FRANCHISE -> {
                FranchiseTabContent(
                    allGames = allGames,
                    innerPadding = innerPadding,
                    selectedFranchiseName = selectedFranchiseName,
                    onSelectFranchise = { selectedFranchiseName = it },
                    onSelectGame = { viewModel.onSelectGame(it) }
                )
            }
            MainBottomNavTab.ACCOUNT -> {
                AccountTabContent(
                    allGames = allGames,
                    userProfile = userProfile,
                    syncStatus = syncStatus,
                    innerPadding = innerPadding,
                    onOpenAccountDialog = { viewModel.openAccountDialog() },
                    onOpenBackupSheet = { viewModel.openBackupSheet() },
                    onOpenRandomGame = { viewModel.openRandomGameDialog() }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    gameToDelete?.let { game ->
        AlertDialog(
            onDismissRequest = { gameToDelete = null },
            title = {
                Text(
                    text = "Delete Game?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5EEF8)
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${game.title}\" (${game.console.displayName}) from your collection?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFCAC4D0)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGame(game)
                        gameToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBA1A1A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_dialog_btn")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { gameToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_dialog_btn")
                ) {
                    Text("Cancel", color = Color(0xFFD0BCFF))
                }
            },
            containerColor = Color(0xFF2B2930),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("delete_confirmation_dialog")
        )
    }

    // Pick Random Game Modal Dialog
    if (randomSelectionState.isOpen) {
        RandomGameDialog(
            state = randomSelectionState,
            onPlatformSelected = { viewModel.onRandomPlatformSelected(it) },
            onConsoleSelected = { viewModel.onRandomConsoleSelected(it) },
            onStatusSelected = { viewModel.onRandomStatusSelected(it) },
            onReRoll = { viewModel.reRollRandomGame() },
            onViewGameDetails = { game ->
                viewModel.closeRandomGameDialog()
                viewModel.onSelectGame(game)
            },
            onDismiss = { viewModel.closeRandomGameDialog() }
        )
    }

    // Selected Game Detail Modal Bottom Sheet (Embedded YouTube Player, 3D Disc Flip & Time to Beat)
    selectedGame?.let { game ->
        GameDetailSheet(
            game = game,
            onDismiss = { viewModel.onSelectGame(null) },
            onEdit = { g ->
                viewModel.onSelectGame(null)
                viewModel.openEditGameDialog(g)
            },
            onDelete = { g ->
                viewModel.deleteGame(g)
            },
            onToggleFavorite = { id ->
                viewModel.toggleFavorite(id)
            },
            onUpdateStatus = { id, newStatus ->
                viewModel.updateStatus(id, newStatus)
            },
            onUpdateRating = { id, newRating ->
                viewModel.updateRating(id, newRating)
            },
            loadingStatus = hltbLoadingStates[game.id] ?: com.example.data.hltb.HltbLoadingStatus.IDLE,
            onRefreshHltb = { g ->
                viewModel.fetchHltbForGame(g, forceRefresh = true)
            },
            isRefreshingMetadata = isRefreshingMetadata[game.id] == true,
            onRefreshMetadata = { g ->
                viewModel.refreshMetadataForGame(g)
            },
            onOpenVersus = { g ->
                viewModel.onSelectGame(null)
                viewModel.openVersusPicker(g)
            },
            onUpdateBackCover = { id, path ->
                viewModel.updateBackCover(id, path)
            },
            onApplyCoverVariant = { id, variant ->
                viewModel.applyCoverVariant(id, variant)
            },
            onApplySteamGridDbCover = { id, fullUrl, thumbUrl ->
                viewModel.applySteamGridDbCover(id, fullUrl, thumbUrl)
            },
            onRefreshCover = { g ->
                viewModel.refreshGameCover(g)
            }
        )
    }

    // Versus / Choice Picker Modal Bottom Sheet (Decisive comparison & AI recommendation)
    if (versusState.isOpen) {
        VersusChoiceDialog(
            state = versusState,
            allGames = allGames,
            onToggleGame = { game -> viewModel.toggleVersusGame(game) },
            onRemoveGame = { id -> viewModel.removeVersusGame(id) },
            onClearSelection = { viewModel.clearVersusSelection() },
            onSearchQueryChange = { q -> viewModel.onVersusSearchQueryChange(q) },
            onConsoleFilterChange = { c -> viewModel.onVersusConsoleFilterChange(c) },
            onStatusFilterChange = { s -> viewModel.onVersusStatusFilterChange(s) },
            onRunComparison = { viewModel.runVersusComparison() },
            onResetComparison = { viewModel.resetVersusComparison() },
            onViewGameDetails = { game ->
                viewModel.closeVersusPicker()
                viewModel.onSelectGame(game)
            },
            onSetStatusToPlaying = { gameId ->
                viewModel.updateStatus(gameId, "PLAYING")
            },
            onDismiss = { viewModel.closeVersusPicker() }
        )
    }

    // Dedicated YouTube Trailer Embedded Modal Dialog
    trailerGame?.let { game ->
        YouTubeTrailerDialog(
            game = game,
            onDismiss = { trailerGame = null },
            onOpenDetail = {
                trailerGame = null
                viewModel.onSelectGame(game)
            }
        )
    }

    // User Account & Email Login Dialog
    if (isAccountDialogOpen) {
        UserAccountDialog(
            userProfile = userProfile,
            libraryStats = libraryStats,
            syncStatus = syncStatus,
            onSyncNow = { viewModel.syncNow() },
            onOpenBackup = { viewModel.openBackupSheet() },
            onLogin = { email, name ->
                viewModel.loginWithEmail(email, name)
                viewModel.closeAccountDialog()
            },
            onLogout = {
                viewModel.logout()
            },
            onDismiss = {
                viewModel.closeAccountDialog()
            }
        )
    }

    // Export & Backup Bottom Sheet
    if (isBackupSheetOpen) {
        ExportBackupSheet(
            games = allGames,
            userProfile = userProfile,
            syncStatus = syncStatus,
            backupState = backupOperationState,
            onSyncNow = { viewModel.syncNow() },
            onExport = { format -> viewModel.exportLibrary(format) },
            onAnalyzeFileUri = { ctx, uri -> viewModel.analyzeBackupFromUri(ctx, uri) },
            onAnalyzeJsonText = { text -> viewModel.analyzeBackupContent(text) },
            onConfirmRestore = { analysis, strategy -> viewModel.confirmRestore(analysis, strategy) },
            onCancelImportPreview = { viewModel.cancelImportPreview() },
            onResetBackupState = { viewModel.resetBackupState() },
            onSaveToUri = { ctx, uri, content, callback -> viewModel.saveContentToUri(ctx, uri, content, callback) },
            onDismiss = { viewModel.closeBackupSheet() }
        )
    }

    // Add / Edit Game Dialog
    if (isAddDialogOpen) {
        AddEditGameDialog(
            gameToEdit = gameToEdit,
            initialConsole = selectedConsole,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { game ->
                viewModel.saveGame(game)
            }
        )
    }
}

@Composable
fun ActiveFilterTag(
    label: String,
    onRemove: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(16.dp)),
        color = Color(0xFF381E72),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFFEADDFF)
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove filter",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun HeroVaultBanner(
    featuredGame: Game?,
    onWatchFeaturedTrailer: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFD0BCFF)),
        color = Color(0xFF2B2930),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Art
            Image(
                painter = painterResource(id = R.drawable.img_library_hero),
                contentDescription = "Library Vault Hero Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            // Dark gradient overlay for text legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1C1B1F).copy(alpha = 0.94f),
                                Color(0xFF1C1B1F).copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Content inside hero banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFD0BCFF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FEATURED TRAILER",
                                color = Color(0xFF381E72),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (featuredGame != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = featuredGame.console.shortName,
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = featuredGame?.title ?: "Explore Your Game Wishlist",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE6E1E5)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = featuredGame?.genre ?: "Organize by console with disc covers & YouTube trailers",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFCAC4D0),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (featuredGame != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD0BCFF))
                                .clickable { onWatchFeaturedTrailer(featuredGame) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch Trailer",
                                tint = Color(0xFF381E72),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Watch Trailer & View Details",
                                color = Color(0xFF381E72),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameListItemCard(
    game: Game,
    onClick: () -> Unit,
    onTrailerClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val console = game.console

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("list_game_card_${game.id}")
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF2B2930),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature Disc Cover
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(95.dp)
            ) {
                DiscCoverCard(
                    game = game,
                    showDiscPeeking = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Game Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF4A4458)
                        ) {
                            Text(
                                text = "${console.emoji} ${console.shortName}",
                                color = Color(0xFFE8DEF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        val emu = game.emulator.ifBlank { console.primaryEmulator() }
                        if (emu.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF381E72)
                            ) {
                                Text(
                                    text = "🎮 $emu",
                                    color = Color(0xFFEADDFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("list_delete_game_${game.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete ${game.title}",
                                    tint = Color(0xFFFFB4AB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E5)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = game.genre,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFCAC4D0)
                    ),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF36343B))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${game.status.iconEmoji} ${game.status.label}",
                            color = Color(0xFFE6E1E5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Watch Trailer Play Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD0BCFF).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable {
                                (onTrailerClick ?: onClick).invoke()
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("list_trailer_btn_${game.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Trailer",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Trailer",
                            color = Color(0xFFD0BCFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, count: Int, color: Color, emoji: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF2B2930),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = Color(0xFFCAC4D0)
                    )
                )
            }
        }
    }
}

@Composable
fun StatusFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeColor: Color = Color(0xFFD0BCFF),
    testTag: String
) {
    Surface(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF2B2930),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.5.sp
            ),
            color = if (isSelected) Color(0xFF381E72) else Color(0xFFCAC4D0),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AddGameQuickSection(
    gameName: String,
    onGameNameChange: (String) -> Unit,
    isDetecting: Boolean,
    statusMessage: String?,
    onDetectAndAdd: () -> Unit,
    onOpenFullAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFD0BCFF)),
        color = Color(0xFF262033),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Game",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "QUICK ADD GAME",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD0BCFF),
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = "Auto console, cover & trailer",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFCAC4D0),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Game Name Input & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = gameName,
                    onValueChange = onGameNameChange,
                    placeholder = {
                        Text(
                            "Enter game name (e.g. Persona 4, Elden Ring...)",
                            fontSize = 12.5.sp,
                            color = Color(0xFF938F99)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDetectAndAdd() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_game_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedContainerColor = Color(0xFF1E182A),
                        unfocusedContainerColor = Color(0xFF1E182A),
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = onDetectAndAdd,
                    enabled = !isDetecting && gameName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72),
                        disabledContainerColor = Color(0xFF382E4D),
                        disabledContentColor = Color(0xFF8B829C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("quick_add_game_btn")
                ) {
                    if (isDetecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF381E72),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFD0BCFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun EmptyWishlistState(
    searchQuery: String,
    selectedConsole: GameConsole,
    selectedStatus: String = "ALL",
    activeFilterCount: Int = 0,
    isDetecting: Boolean = false,
    statusMessage: String? = null,
    onResetFilters: () -> Unit,
    onAddWithAi: (String) -> Unit = {},
    onAddGame: () -> Unit
) {
    var inlineGameName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF36343B))
                .border(1.dp, Color(0xFF49454F), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (searchQuery.isNotEmpty()) "🔍" else "🎮",
                fontSize = 32.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                searchQuery.isNotEmpty() -> "No games matching \"$searchQuery\""
                activeFilterCount > 0 -> "No games match active filters"
                selectedConsole != GameConsole.ALL -> "No games in ${selectedConsole.displayName}"
                else -> "Your Wishlist Vault is Empty"
            },
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6E1E5)
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (searchQuery.isNotEmpty() || activeFilterCount > 0) {
                "Try clearing your search query or filters to view all games."
            } else {
                "Enter any game title below to automatically find its console, cover art, and official trailer."
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFCAC4D0),
                lineHeight = 18.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // If library is empty without search query, provide quick inline add box
        if (searchQuery.isEmpty() && activeFilterCount == 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF6750A4), RoundedCornerShape(14.dp)),
                color = Color(0xFF262033),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Add Your First Game",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inlineGameName,
                            onValueChange = { inlineGameName = it },
                            placeholder = { Text("e.g. Silent Hill 2, Crisis Core...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("empty_state_game_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedContainerColor = Color(0xFF1E182A),
                                unfocusedContainerColor = Color(0xFF1E182A),
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (inlineGameName.isNotBlank()) {
                                    onAddWithAi(inlineGameName)
                                    inlineGameName = ""
                                }
                            },
                            enabled = !isDetecting && inlineGameName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (isDetecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF381E72),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD0BCFF))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (activeFilterCount > 0 || searchQuery.isNotEmpty()) {
                Button(
                    onClick = onResetFilters,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF36343B),
                        contentColor = Color(0xFFE6E1E5)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_empty_state_reset_filters")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Reset Filters",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Filters")
                }
            }

            Button(
                onClick = onAddGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_empty_state_add_game")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open Add Game Dialog", fontWeight = FontWeight.Bold)
            }
        }
    }
}

