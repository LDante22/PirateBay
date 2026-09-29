package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.DetectionSource
import com.example.data.ai.GameAiDetector
import com.example.data.auth.UserProfile
import com.example.data.auth.UserSessionManager
import com.example.data.backup.BackupOperationState
import com.example.data.backup.ConflictResolutionStrategy
import com.example.data.backup.ExportFormat
import com.example.data.backup.ImportAnalysisResult
import com.example.data.backup.LibraryBackupService
import com.example.data.db.AppDatabase
import com.example.data.db.DefaultSeedData
import com.example.data.hltb.HltbLoadingStatus
import com.example.data.hltb.HltbMatchResult
import com.example.data.hltb.HltbRepository
import com.example.data.metadata.CoverProjectVariant
import com.example.data.metadata.ExternalGameMetadata
import com.example.data.metadata.ExternalMetadataSearchResult
import com.example.data.metadata.GameCoverFetcher
import com.example.data.metadata.GameMetadataRepository
import com.example.data.metadata.MetadataOperationStatus
import com.example.data.metadata.MetadataSearchState
import com.example.data.model.CoverSourceType
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.ShelfViewMode
import com.example.data.model.WishlistStatus
import com.example.data.repository.GameRepository
import com.example.data.sync.FirestoreSyncManager
import com.example.data.sync.SyncStatus
import com.example.data.versus.VersusComparisonEngine
import com.example.data.versus.VersusResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SortOption(val id: String, val label: String) {
    RECENT("RECENT", "Recently Added"),
    TITLE("TITLE", "Title (A-Z)"),
    YEAR("YEAR", "Release Year (Newest)"),
    YEAR_ASC("YEAR_ASC", "Release Year (Oldest)"),
    RATING("RATING", "Highest Rating")
}

data class YearEra(
    val id: String,
    val label: String,
    val minYear: Int,
    val maxYear: Int,
    val description: String
)

val PRESET_YEAR_ERAS = listOf(
    YearEra("ERA_ALL", "All Eras", 0, 0, "All release years"),
    YearEra("ERA_2000_2005", "2000–2005", 2000, 2005, "PS2, GameCube, Classic PC"),
    YearEra("ERA_2006_2010", "2006–2010", 2006, 2010, "PSP, PS3, Nintendo Wii"),
    YearEra("ERA_2011_2015", "2011–2015", 2011, 2015, "PS Vita, 3DS, Late PS3"),
    YearEra("ERA_2016_2020", "2016–2020", 2016, 2020, "Switch Era & Modern PC"),
    YearEra("ERA_2021_PRESENT", "2021–Now", 2021, 2030, "Latest Releases")
)

data class LibraryStats(
    val totalCount: Int = 0,
    val wantToPlayCount: Int = 0,
    val priorityCount: Int = 0,
    val ownedCount: Int = 0,
    val playingCount: Int = 0
)

class GameVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository = GameRepository(AppDatabase.getDatabase(application).gameDao())
    private val userSessionManager = UserSessionManager(application)
    private val firestoreSyncManager = FirestoreSyncManager(application)
    private val hltbRepository: HltbRepository = HltbRepository()

    val userProfile: StateFlow<UserProfile> = userSessionManager.userProfile
    val syncStatus: StateFlow<SyncStatus> = firestoreSyncManager.syncStatus
    val hltbLoadingStates = MutableStateFlow<Map<Long, HltbLoadingStatus>>(emptyMap())

    val searchQuery = MutableStateFlow("")
    val selectedConsole = MutableStateFlow(GameConsole.ALL)
    val selectedGenre = MutableStateFlow("ALL")
    val selectedYear = MutableStateFlow(0) // 0 means all years
    val selectedYearEra = MutableStateFlow<YearEra?>(null)
    val selectedStatus = MutableStateFlow("ALL")
    val sortBy = MutableStateFlow(SortOption.RECENT.id)
    val shelfViewMode = MutableStateFlow(userSessionManager.getShelfViewMode())

    val selectedGame = MutableStateFlow<Game?>(null)
    val gameToEdit = MutableStateFlow<Game?>(null)
    val isAddDialogOpen = MutableStateFlow(false)
    val isAccountDialogOpen = MutableStateFlow(false)
    val isBackupSheetOpen = MutableStateFlow(false)
    val backupOperationState = MutableStateFlow<BackupOperationState>(BackupOperationState.Idle)

    // Random Game Selection State
    val randomSelectionState = MutableStateFlow(RandomSelectionState())
    private var lastRandomGameId: Long? = null

    // Versus / Choice Picker State
    val versusState = MutableStateFlow(VersusSelectionState())

    // AI Detection State
    val isAiDetecting = MutableStateFlow(false)
    val aiDetectionStatus = MutableStateFlow<String?>(null)
    val aiDetectedGame = MutableStateFlow<Game?>(null)
    val aiDetectionSource = MutableStateFlow<DetectionSource?>(null)

    // External Metadata Search & Integration State
    val metadataSearchState = MutableStateFlow(MetadataSearchState())
    val isRefreshingMetadata = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    val allGames: StateFlow<List<Game>> = repository.getAllGames()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        // Automatically trigger cloud sync when a user logs in or profile changes
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile.isLoggedIn && profile.email.isNotBlank()) {
                    triggerCloudSync(profile.email)
                } else {
                    firestoreSyncManager.stopListening()
                }
            }
        }

        // Automatic background cover art healer for existing library items
        viewModelScope.launch {
            allGames.collect { games ->
                val missingCovers = games.filter { it.coverArtUrl.isBlank() }
                if (missingCovers.isNotEmpty()) {
                    for (missing in missingCovers) {
                        launch {
                            healGameCover(missing)
                        }
                    }
                }
            }
        }
    }

    private fun triggerCloudSync(email: String) {
        val currentGames = allGames.value
        firestoreSyncManager.startSyncForUser(
            userEmail = email,
            localGames = currentGames,
            scope = viewModelScope,
            onMergeRemoteGames = { remoteGames ->
                for (remoteGame in remoteGames) {
                    val local = repository.getGameById(remoteGame.id)
                    if (local == null) {
                        repository.insertGame(remoteGame)
                    } else if (remoteGame.addedTimestamp >= local.addedTimestamp) {
                        repository.updateGame(remoteGame)
                    }
                }
            }
        )
    }

    fun syncNow() {
        val email = userProfile.value.email
        if (email.isNotBlank()) {
            triggerCloudSync(email)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredGames: StateFlow<List<Game>> = combine(
        combine(searchQuery, selectedConsole, selectedStatus) { query, console, status ->
            Triple(query, console, status)
        },
        combine(selectedGenre, selectedYear, selectedYearEra) { genre, year, yearEra ->
            Triple(genre, year, yearEra)
        },
        sortBy
    ) { (query, console, status), (genre, year, yearEra), sort ->
        val minYear = if (year != 0) 0 else yearEra?.minYear ?: 0
        val maxYear = if (year != 0) 0 else yearEra?.maxYear ?: 0
        val effectiveYear = if (year != 0) year else 0
        FilterParams(
            query = query.trim(),
            consoleId = console.id,
            genre = genre,
            year = effectiveYear,
            minYear = minYear,
            maxYear = maxYear,
            status = status,
            sortBy = sort
        )
    }.flatMapLatest { params ->
        repository.searchAndFilterGames(
            query = params.query,
            consoleId = params.consoleId,
            genre = params.genre,
            year = params.year,
            minYear = params.minYear,
            maxYear = params.maxYear,
            status = params.status,
            sortBy = params.sortBy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeFilterCount: StateFlow<Int> = combine(
        combine(searchQuery, selectedConsole, selectedStatus) { query, console, status ->
            var count = 0
            if (query.isNotBlank()) count++
            if (console != GameConsole.ALL) count++
            if (status != "ALL") count++
            count
        },
        combine(selectedGenre, selectedYear, selectedYearEra) { genre, year, yearEra ->
            var count = 0
            if (genre.isNotBlank() && genre != "ALL") count++
            if (year != 0 || (yearEra != null && yearEra.id != "ERA_ALL")) count++
            count
        }
    ) { baseCount, genreYearCount ->
        baseCount + genreYearCount
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val consoleCounts: StateFlow<Map<String, Int>> = allGames.combine(MutableStateFlow(Unit)) { games, _ ->
        val map = mutableMapOf<String, Int>()
        map[GameConsole.ALL.id] = games.size
        GameConsole.entries.forEach { console ->
            if (console != GameConsole.ALL) {
                map[console.id] = games.count { it.consoleId == console.id }
            }
        }
        map
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val libraryStats: StateFlow<LibraryStats> = allGames.combine(MutableStateFlow(Unit)) { games, _ ->
        LibraryStats(
            totalCount = games.size,
            wantToPlayCount = games.count { it.wishlistStatus == WishlistStatus.WANT_TO_PLAY.id },
            priorityCount = games.count { it.wishlistStatus == WishlistStatus.TOP_PRIORITY.id },
            ownedCount = games.count { it.wishlistStatus == WishlistStatus.ACQUIRED.id },
            playingCount = games.count { it.wishlistStatus == WishlistStatus.PLAYING.id }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryStats()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onConsoleSelected(console: GameConsole) {
        selectedConsole.value = console
    }

    fun onGenreSelected(genre: String) {
        selectedGenre.value = genre
    }

    fun onYearSelected(year: Int) {
        selectedYear.value = year
        if (year != 0) {
            selectedYearEra.value = null
        }
    }

    fun onYearEraSelected(era: YearEra?) {
        selectedYearEra.value = era
        if (era != null && era.id != "ERA_ALL") {
            selectedYear.value = 0
        }
    }

    fun onStatusSelected(status: String) {
        selectedStatus.value = status
    }

    fun onSortBySelected(sortOptionId: String) {
        sortBy.value = sortOptionId
    }

    fun setShelfViewMode(mode: ShelfViewMode) {
        shelfViewMode.value = mode
        userSessionManager.setShelfViewMode(mode)
    }

    fun resetAllFilters() {
        searchQuery.value = ""
        selectedConsole.value = GameConsole.ALL
        selectedGenre.value = "ALL"
        selectedYear.value = 0
        selectedYearEra.value = null
        selectedStatus.value = "ALL"
    }

    fun onSelectGame(game: Game?) {
        selectedGame.value = game
        if (game != null) {
            // Lazy load HLTB playtime data if not previously retrieved or expired
            val needsResolution = game.hltbLastUpdated == 0L || game.hltbSyncStatus.isBlank()
            if (needsResolution && !game.hasHltbData) {
                fetchHltbForGame(game, forceRefresh = false)
            }
        }
    }

    fun fetchHltbForGame(game: Game, forceRefresh: Boolean = false) {
        if (game.id <= 0L) return
        if (hltbLoadingStates.value[game.id] == HltbLoadingStatus.LOADING) return

        viewModelScope.launch {
            hltbLoadingStates.value = hltbLoadingStates.value + (game.id to HltbLoadingStatus.LOADING)

            val result = hltbRepository.resolveHltbForGame(game, forceRefresh = forceRefresh)
            val now = System.currentTimeMillis()

            when (result) {
                is HltbMatchResult.Matched -> {
                    val entry = result.entry
                    repository.updateHltbData(
                        id = game.id,
                        hltbId = entry.gameId,
                        hltbName = entry.gameName,
                        mainStory = entry.mainStoryHours,
                        mainExtra = entry.mainExtraHours,
                        completionist = entry.completionistHours,
                        lastUpdated = now,
                        syncStatus = "SYNCED"
                    )
                    hltbLoadingStates.value = hltbLoadingStates.value + (game.id to HltbLoadingStatus.SYNCED)

                    val updated = repository.getGameById(game.id)
                    if (selectedGame.value?.id == game.id && updated != null) {
                        selectedGame.value = updated
                    }

                    val email = userProfile.value.email
                    if (updated != null && email.isNotBlank()) {
                        firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
                    }
                }
                is HltbMatchResult.NotFound -> {
                    repository.updateHltbData(
                        id = game.id,
                        hltbId = 0L,
                        hltbName = "",
                        mainStory = 0f,
                        mainExtra = 0f,
                        completionist = 0f,
                        lastUpdated = now,
                        syncStatus = "NOT_FOUND"
                    )
                    hltbLoadingStates.value = hltbLoadingStates.value + (game.id to HltbLoadingStatus.NOT_FOUND)
                    val updated = repository.getGameById(game.id)
                    if (selectedGame.value?.id == game.id && updated != null) {
                        selectedGame.value = updated
                    }
                }
                is HltbMatchResult.Unresolved -> {
                    repository.updateHltbData(
                        id = game.id,
                        hltbId = 0L,
                        hltbName = "",
                        mainStory = 0f,
                        mainExtra = 0f,
                        completionist = 0f,
                        lastUpdated = now,
                        syncStatus = "UNRESOLVED"
                    )
                    hltbLoadingStates.value = hltbLoadingStates.value + (game.id to HltbLoadingStatus.UNRESOLVED)
                    val updated = repository.getGameById(game.id)
                    if (selectedGame.value?.id == game.id && updated != null) {
                        selectedGame.value = updated
                    }
                }
                is HltbMatchResult.Error -> {
                    if (!game.hasHltbData) {
                        repository.updateHltbData(
                            id = game.id,
                            hltbId = game.hltbId,
                            hltbName = game.hltbName,
                            mainStory = game.hltbMainStoryHours,
                            mainExtra = game.hltbMainExtraHours,
                            completionist = game.hltbCompletionistHours,
                            lastUpdated = game.hltbLastUpdated,
                            syncStatus = "ERROR"
                        )
                    }
                    hltbLoadingStates.value = hltbLoadingStates.value + (game.id to HltbLoadingStatus.ERROR)
                    val updated = repository.getGameById(game.id)
                    if (selectedGame.value?.id == game.id && updated != null) {
                        selectedGame.value = updated
                    }
                }
            }
        }
    }

    fun openAccountDialog() {
        isAccountDialogOpen.value = true
    }

    fun closeAccountDialog() {
        isAccountDialogOpen.value = false
    }

    fun loginWithEmail(email: String, name: String = "") {
        val previousEmail = userProfile.value.email
        val cleanEmail = email.trim()
        viewModelScope.launch {
            if (previousEmail.isNotBlank() && !previousEmail.equals(cleanEmail, ignoreCase = true)) {
                // Switching accounts: unsubscribe previous listener and purge previous local cache
                firestoreSyncManager.stopListening()
                repository.deleteAllGames()
                selectedGame.value = null
                gameToEdit.value = null
            }
            userSessionManager.login(cleanEmail, name)
        }
    }

    fun logout() {
        viewModelScope.launch {
            firestoreSyncManager.stopListening()
            repository.deleteAllGames()
            selectedGame.value = null
            gameToEdit.value = null
            userSessionManager.logout()
        }
    }

    fun openAddGameDialog() {
        gameToEdit.value = null
        isAddDialogOpen.value = true
    }

    fun openEditGameDialog(game: Game) {
        gameToEdit.value = game
        isAddDialogOpen.value = true
    }

    fun closeDialogs() {
        isAddDialogOpen.value = false
        gameToEdit.value = null
    }

    fun saveGame(game: Game) {
        viewModelScope.launch {
            val email = userProfile.value.email
            val gameToSave = if (game.userEmail.isBlank() && email.isNotBlank()) {
                game.copy(userEmail = email)
            } else {
                game
            }

            val savedGame = if (gameToSave.id == 0L) {
                val newId = repository.insertGame(gameToSave)
                val inserted = repository.getGameById(newId)
                if (inserted != null) {
                    selectedGame.value = inserted
                }
                inserted ?: gameToSave.copy(id = newId)
            } else {
                repository.updateGame(gameToSave)
                if (selectedGame.value?.id == gameToSave.id) {
                    selectedGame.value = gameToSave
                }
                gameToSave
            }

            // Sync to Firestore
            if (email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, savedGame, viewModelScope)
            }

            // If game is missing cover art, automatically resolve and persist in background
            if (savedGame.coverArtUrl.isBlank()) {
                launch {
                    healGameCover(savedGame)
                }
            }

            // Automatically resolve HowLongToBeat completion times in background
            if (!savedGame.hasHltbData && savedGame.hltbLastUpdated == 0L) {
                launch {
                    fetchHltbForGame(savedGame, forceRefresh = false)
                }
            }

            closeDialogs()
        }
    }

    /**
     * Resolves and updates missing cover art (front, back, and complete physical case insert) for a game record.
     */
    private suspend fun healGameCover(game: Game) {
        if (game.id <= 0L) return
        val resolution = GameCoverFetcher.resolveCoverWithDetails(
            title = game.title,
            console = game.console,
            releaseYear = game.releaseYear,
            existingCoverUrl = game.coverArtUrl,
            existingCoverSource = game.coverSource,
            existingBackCoverUrl = game.backCoverUrl,
            existingCompleteCaseArtwork = game.completeCaseArtwork
        )
        if (resolution.coverUrl.isNotBlank() || resolution.backCoverUrl.isNotBlank() || resolution.completeCaseArtwork.isNotBlank()) {
            val updated = game.copy(
                coverArtUrl = resolution.coverUrl.ifBlank { game.coverArtUrl },
                coverSource = resolution.coverSource.ifBlank { game.coverSource },
                backCoverUrl = resolution.backCoverUrl.ifBlank { game.backCoverUrl },
                backCoverSource = resolution.backCoverSource.ifBlank { game.backCoverSource },
                completeCaseArtwork = resolution.completeCaseArtwork.ifBlank { game.completeCaseArtwork },
                bannerArtUrl = if (game.bannerArtUrl.isBlank()) resolution.coverUrl else game.bannerArtUrl
            )
            repository.updateGame(updated)
            if (selectedGame.value?.id == game.id) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    /**
     * Public manual trigger to refresh or fetch cover art for a specific game (front, back, and complete insert).
     */
    fun refreshGameCover(game: Game) {
        refreshGameCover(game.id)
    }

    fun refreshGameCover(gameId: Long) {
        viewModelScope.launch {
            val game = repository.getGameById(gameId) ?: return@launch
            val resolution = GameCoverFetcher.resolveCoverWithDetails(
                title = game.title,
                console = game.console,
                releaseYear = game.releaseYear,
                existingCoverUrl = game.coverArtUrl,
                existingCoverSource = game.coverSource,
                existingBackCoverUrl = game.backCoverUrl,
                existingCompleteCaseArtwork = game.completeCaseArtwork,
                forceRefresh = true
            )
            if (resolution.coverUrl.isNotBlank() || resolution.backCoverUrl.isNotBlank() || resolution.completeCaseArtwork.isNotBlank()) {
                val updated = game.copy(
                    coverArtUrl = resolution.coverUrl.ifBlank { game.coverArtUrl },
                    coverThumbUrl = resolution.thumbUrl.ifBlank { game.coverThumbUrl },
                    coverSource = resolution.coverSource.ifBlank { game.coverSource },
                    backCoverUrl = resolution.backCoverUrl.ifBlank { game.backCoverUrl },
                    backCoverSource = resolution.backCoverSource.ifBlank { game.backCoverSource },
                    completeCaseArtwork = resolution.completeCaseArtwork.ifBlank { game.completeCaseArtwork },
                    bannerArtUrl = if (game.bannerArtUrl.isBlank()) resolution.coverUrl else game.bannerArtUrl
                )
                repository.updateGame(updated)
                if (selectedGame.value?.id == game.id) {
                    selectedGame.value = updated
                }
                val email = userProfile.value.email
                if (email.isNotBlank()) {
                    firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
                }
            }
        }
    }

    /**
     * Applies a SteamGridDB cover selection (full cover URL, thumbnail URL) to a game record.
     */
    fun applySteamGridDbCover(gameId: Long, fullCoverUrl: String, thumbUrl: String) {
        viewModelScope.launch {
            val game = repository.getGameById(gameId) ?: return@launch
            val updated = game.copy(
                coverArtUrl = fullCoverUrl,
                coverThumbUrl = thumbUrl,
                coverSource = CoverSourceType.STEAMGRIDDB
            )
            repository.updateGame(updated)
            if (selectedGame.value?.id == game.id) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    /**
     * Manually updates or imports a complete physical case insert (Front + Spine + Back)
     * as selected by the user.
     */
    fun updateCompleteCaseArtwork(gameId: Long, completeCaseArtworkUrl: String) {
        viewModelScope.launch {
            val game = repository.getGameById(gameId) ?: return@launch
            val updated = game.copy(
                completeCaseArtwork = completeCaseArtworkUrl,
                coverSource = CoverSourceType.MANUAL
            )
            repository.updateGame(updated)
            if (selectedGame.value?.id == game.id) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    /**
     * Updates or imports the back cover image for a game record.
     */
    fun updateBackCover(gameId: Long, backCoverUrl: String, backCoverSource: String = CoverSourceType.MANUAL) {
        viewModelScope.launch {
            val game = repository.getGameById(gameId) ?: return@launch
            val updated = game.copy(
                backCoverUrl = backCoverUrl,
                backCoverSource = if (backCoverUrl.isNotBlank()) backCoverSource else ""
            )
            repository.updateGame(updated)
            if (selectedGame.value?.id == game.id) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    /**
     * Applies a verified The Cover Project cover variant (front, spine, and back) to a game.
     */
    fun applyCoverVariant(gameId: Long, variant: CoverProjectVariant) {
        viewModelScope.launch {
            val game = repository.getGameById(gameId) ?: return@launch
            val updated = game.copy(
                coverArtUrl = variant.frontCoverUrl.ifBlank { game.coverArtUrl },
                backCoverUrl = variant.backCoverUrl.ifBlank { if (variant.isCompleteWrap) variant.completeCaseArtworkUrl else "" },
                completeCaseArtwork = if (variant.isCompleteWrap) variant.completeCaseArtworkUrl else game.completeCaseArtwork,
                coverSource = CoverSourceType.COVER_PROJECT,
                backCoverSource = CoverSourceType.COVER_PROJECT
            )
            repository.updateGame(updated)
            if (selectedGame.value?.id == game.id) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    /**
     * Re-scans and heals any games in the library with missing cover art.
     */
    fun healMissingCovers() {
        viewModelScope.launch {
            val games = allGames.value.filter { it.coverArtUrl.isBlank() }
            for (game in games) {
                launch {
                    healGameCover(game)
                }
            }
        }
    }

    fun deleteGame(game: Game) {
        viewModelScope.launch {
            repository.deleteGame(game)
            val email = userProfile.value.email
            if (email.isNotBlank()) {
                firestoreSyncManager.deleteGameFromFirestore(email, game.id, viewModelScope)
            }
            if (selectedGame.value?.id == game.id) {
                selectedGame.value = null
            }
        }
    }

    fun clearAllGames() {
        viewModelScope.launch {
            val email = userProfile.value.email
            val gamesToDelete = allGames.value
            repository.deleteAllGames()
            if (email.isNotBlank()) {
                for (g in gamesToDelete) {
                    firestoreSyncManager.deleteGameFromFirestore(email, g.id, viewModelScope)
                }
            }
            selectedGame.value = null
        }
    }

    fun toggleFavorite(gameId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(gameId)
            val updated = repository.getGameById(gameId)
            if (selectedGame.value?.id == gameId) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (updated != null && email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    fun updateStatus(gameId: Long, newStatus: String) {
        viewModelScope.launch {
            repository.updateStatus(gameId, newStatus)
            val updated = repository.getGameById(gameId)
            if (selectedGame.value?.id == gameId) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (updated != null && email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    fun updateRating(gameId: Long, rating: Float) {
        viewModelScope.launch {
            repository.updateRating(gameId, rating)
            val updated = repository.getGameById(gameId)
            if (selectedGame.value?.id == gameId) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (updated != null && email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    fun updateEmulatorNotes(
        gameId: Long,
        notes: String,
        backend: String = "",
        resolution: String = "",
        bios: String = "",
        framerate: String = "",
        proTips: String = ""
    ) {
        viewModelScope.launch {
            repository.updateEmulatorNotes(gameId, notes, backend, resolution, bios, framerate, proTips)
            val updated = repository.getGameById(gameId)
            if (selectedGame.value?.id == gameId) {
                selectedGame.value = updated
            }
            val email = userProfile.value.email
            if (updated != null && email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
            }
        }
    }

    fun detectGameWithAi(gameTitle: String, onComplete: ((Game) -> Unit)? = null) {
        val cleanTitle = gameTitle.trim()
        if (cleanTitle.isBlank()) return

        viewModelScope.launch {
            isAiDetecting.value = true
            aiDetectionStatus.value = "Finding game & console info..."
            try {
                val result = GameAiDetector.detectGame(cleanTitle)
                aiDetectedGame.value = result.game
                aiDetectionSource.value = result.source
                aiDetectionStatus.value = "Found: ${result.game.console.displayName} • Trailer & Cover ready"
                onComplete?.invoke(result.game)
            } catch (e: Exception) {
                aiDetectionStatus.value = "Details ready"
                val fallbackGame = GameAiDetector.findInCatalogOrSynthesize(cleanTitle)
                aiDetectedGame.value = fallbackGame
                onComplete?.invoke(fallbackGame)
            } finally {
                isAiDetecting.value = false
            }
        }
    }

    fun clearAiDetection() {
        aiDetectedGame.value = null
        aiDetectionStatus.value = null
        aiDetectionSource.value = null
        isAiDetecting.value = false
    }

    fun addAiDetectedGame(game: Game) {
        viewModelScope.launch {
            val email = userProfile.value.email
            val gameToSave = if (game.userEmail.isBlank() && email.isNotBlank()) {
                game.copy(userEmail = email)
            } else {
                game
            }
            val newId = repository.insertGame(gameToSave)
            val inserted = repository.getGameById(newId)
            val finalGame = inserted ?: gameToSave.copy(id = newId)
            selectedGame.value = finalGame

            if (email.isNotBlank()) {
                firestoreSyncManager.saveGameToFirestore(email, finalGame, viewModelScope)
            }

            clearAiDetection()
            closeDialogs()
        }
    }

    // ==========================================
    // EXTERNAL GAME METADATA INTEGRATION
    // ==========================================

    private var externalSearchJob: Job? = null
    private var metadataDetailsJob: Job? = null

    fun searchExternalMetadata(query: String, targetConsole: GameConsole? = null, debounceMs: Long = 0L) {
        externalSearchJob?.cancel()
        val clean = query.trim()
        if (clean.isBlank() || clean.length < 2) {
            metadataSearchState.value = MetadataSearchState(
                status = MetadataOperationStatus.IDLE,
                query = clean,
                results = emptyList()
            )
            return
        }

        // Immediately reset previous search results and enter searching state
        metadataSearchState.value = MetadataSearchState(
            status = MetadataOperationStatus.SEARCHING,
            query = clean,
            results = emptyList()
        )

        externalSearchJob = viewModelScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
            }
            if (!isActive) return@launch

            val result = GameMetadataRepository.searchGames(clean, targetConsole)
            if (!isActive) return@launch

            result.onSuccess { results ->
                if (!isActive) return@launch
                metadataSearchState.value = MetadataSearchState(
                    status = if (results.isNotEmpty()) MetadataOperationStatus.SUCCESS else MetadataOperationStatus.NOT_FOUND,
                    query = clean,
                    results = results,
                    usingFallback = !GameMetadataRepository.hasValidApiKey()
                )
            }.onFailure { err ->
                if (!isActive) return@launch
                metadataSearchState.value = MetadataSearchState(
                    status = MetadataOperationStatus.NETWORK_ERROR,
                    query = clean,
                    errorMessage = err.localizedMessage ?: "Failed to query external database"
                )
            }
        }
    }

    fun clearMetadataSearch() {
        externalSearchJob?.cancel()
        metadataDetailsJob?.cancel()
        metadataSearchState.value = MetadataSearchState()
    }

    fun fetchAndApplyExternalMetadata(
        externalGameId: String,
        targetConsole: GameConsole? = null,
        onMetadataLoaded: (ExternalGameMetadata) -> Unit
    ) {
        metadataDetailsJob?.cancel()
        metadataDetailsJob = viewModelScope.launch {
            metadataSearchState.value = metadataSearchState.value.copy(status = MetadataOperationStatus.LOADING_DETAILS)
            val result = GameMetadataRepository.fetchGameDetails(externalGameId, targetConsole)
            if (!isActive) return@launch
            result.onSuccess { metadata ->
                if (!isActive) return@launch
                metadataSearchState.value = metadataSearchState.value.copy(status = MetadataOperationStatus.SUCCESS)
                onMetadataLoaded(metadata)
            }.onFailure { err ->
                if (!isActive) return@launch
                metadataSearchState.value = metadataSearchState.value.copy(
                    status = MetadataOperationStatus.NETWORK_ERROR,
                    errorMessage = err.localizedMessage
                )
            }
        }
    }

    fun refreshMetadataForGame(game: Game) {
        if (game.id <= 0L) return
        if (isRefreshingMetadata.value[game.id] == true) return

        viewModelScope.launch {
            isRefreshingMetadata.value = isRefreshingMetadata.value + (game.id to true)
            val result = GameMetadataRepository.refreshGameMetadata(game)
            val now = System.currentTimeMillis()

            result.onSuccess { metadata ->
                repository.updateExternalMetadata(
                    id = game.id,
                    provider = metadata.externalProvider,
                    gameId = metadata.externalGameId,
                    lastUpdated = now,
                    syncStatus = "SYNCED",
                    bannerArtUrl = metadata.bannerArtUrl.ifBlank { game.bannerArtUrl },
                    publisher = metadata.publishers.joinToString(", ").ifBlank { game.publisher },
                    releaseDate = metadata.releaseDate.ifBlank { game.releaseDate },
                    description = metadata.description.ifBlank { game.description },
                    websiteUrl = metadata.websiteUrl.ifBlank { game.websiteUrl },
                    metacriticScore = if (metadata.metacriticScore > 0) metadata.metacriticScore else game.metacriticScore,
                    supportedPlatforms = metadata.platformsDisplay.ifBlank { game.supportedPlatforms }
                )

                val updated = repository.getGameById(game.id)
                if (selectedGame.value?.id == game.id && updated != null) {
                    selectedGame.value = updated
                }

                val email = userProfile.value.email
                if (updated != null && email.isNotBlank()) {
                    firestoreSyncManager.saveGameToFirestore(email, updated, viewModelScope)
                }
            }

            isRefreshingMetadata.value = isRefreshingMetadata.value - game.id
        }
    }

    // ==========================================
    // RANDOM GAME PICKER SYSTEM
    // ==========================================

    fun openRandomGameDialog(
        initialPlatform: PlatformOption? = null,
        initialConsole: GameConsole? = null
    ) {
        val platform = initialPlatform ?: if (initialConsole != null && initialConsole != GameConsole.ALL) {
            PlatformOption.fromConsole(initialConsole)
        } else if (selectedConsole.value != GameConsole.ALL) {
            PlatformOption.fromConsole(selectedConsole.value)
        } else {
            PlatformOption.ALL
        }

        val console = initialConsole ?: if (selectedConsole.value != GameConsole.ALL) selectedConsole.value else null
        val currentStatus = selectedStatus.value

        randomSelectionState.value = RandomSelectionState(
            isOpen = true,
            selectedPlatform = platform,
            selectedConsole = console,
            selectedStatus = currentStatus
        )

        // Automatically trigger initial random pick from eligible games pool
        pickRandomGame(
            platform = platform,
            specificConsole = console,
            statusFilter = currentStatus
        )
    }

    fun closeRandomGameDialog() {
        randomSelectionState.value = randomSelectionState.value.copy(isOpen = false)
    }

    fun onRandomPlatformSelected(platform: PlatformOption) {
        randomSelectionState.value = randomSelectionState.value.copy(
            selectedPlatform = platform,
            selectedConsole = null
        )
        pickRandomGame(
            platform = platform,
            specificConsole = null,
            statusFilter = randomSelectionState.value.selectedStatus
        )
    }

    fun onRandomConsoleSelected(console: GameConsole?) {
        val platform = if (console != null && console != GameConsole.ALL) {
            PlatformOption.fromConsole(console)
        } else {
            PlatformOption.ALL
        }
        randomSelectionState.value = randomSelectionState.value.copy(
            selectedPlatform = platform,
            selectedConsole = console
        )
        pickRandomGame(
            platform = platform,
            specificConsole = console,
            statusFilter = randomSelectionState.value.selectedStatus
        )
    }

    fun onRandomStatusSelected(status: String) {
        randomSelectionState.value = randomSelectionState.value.copy(
            selectedStatus = status
        )
        pickRandomGame(
            platform = randomSelectionState.value.selectedPlatform,
            specificConsole = randomSelectionState.value.selectedConsole,
            statusFilter = status
        )
    }

    fun reRollRandomGame() {
        val state = randomSelectionState.value
        pickRandomGame(
            platform = state.selectedPlatform,
            specificConsole = state.selectedConsole,
            statusFilter = state.selectedStatus
        )
    }

    fun pickRandomGame(
        platform: PlatformOption = PlatformOption.ALL,
        specificConsole: GameConsole? = null,
        statusFilter: String = "ALL"
    ) {
        val currentGames = allGames.value
        val eligibleGames = getEligibleGames(
            games = currentGames,
            platform = platform,
            specificConsole = specificConsole,
            statusFilter = statusFilter
        )

        val targetName = if (specificConsole != null && specificConsole != GameConsole.ALL) {
            specificConsole.displayName
        } else {
            platform.displayName
        }

        if (eligibleGames.isEmpty()) {
            val statusSuffix = if (statusFilter != "ALL") {
                val stLabel = WishlistStatus.fromId(statusFilter).label
                " with status \"$stLabel\""
            } else ""

            val message = if (platform == PlatformOption.ALL && (specificConsole == null || specificConsole == GameConsole.ALL)) {
                if (statusFilter != "ALL") {
                    "You don't have any games in your library$statusSuffix."
                } else {
                    "You don't have any games in your library."
                }
            } else {
                "You don't have any $targetName games in your library$statusSuffix."
            }

            randomSelectionState.value = randomSelectionState.value.copy(
                selectedPlatform = platform,
                selectedConsole = specificConsole,
                selectedStatus = statusFilter,
                resultGame = null,
                eligibleGamesCount = 0,
                emptyStateMessage = message,
                rollCount = randomSelectionState.value.rollCount + 1
            )
        } else {
            val pickedGame = if (eligibleGames.size > 1 && lastRandomGameId != null) {
                val candidatePool = eligibleGames.filter { it.id != lastRandomGameId }
                if (candidatePool.isNotEmpty()) candidatePool.random() else eligibleGames.random()
            } else {
                eligibleGames.random()
            }

            lastRandomGameId = pickedGame.id

            randomSelectionState.value = randomSelectionState.value.copy(
                selectedPlatform = platform,
                selectedConsole = specificConsole,
                selectedStatus = statusFilter,
                resultGame = pickedGame,
                eligibleGamesCount = eligibleGames.size,
                emptyStateMessage = null,
                rollCount = randomSelectionState.value.rollCount + 1
            )
        }
    }

    fun getEligibleGames(
        games: List<Game>,
        platform: PlatformOption,
        specificConsole: GameConsole? = null,
        statusFilter: String = "ALL"
    ): List<Game> {
        return games.filter { game ->
            // 1. Platform / Console filter
            val matchesPlatform = if (specificConsole != null && specificConsole != GameConsole.ALL) {
                game.consoleId.equals(specificConsole.id, ignoreCase = true)
            } else {
                platform.matchesGame(game)
            }
            if (!matchesPlatform) return@filter false

            // 2. Status filter (e.g. Playing, Completed, Backlog, Wishlist)
            if (statusFilter != "ALL" && !game.wishlistStatus.equals(statusFilter, ignoreCase = true)) {
                return@filter false
            }

            true
        }
    }

    // ==========================================
    // VERSUS / CHOICE PICKER SYSTEM
    // ==========================================

    fun openVersusPicker(initialGame: Game? = null) {
        val initialSet = if (initialGame != null) setOf(initialGame.id) else emptySet()
        versusState.value = VersusSelectionState(
            isOpen = true,
            selectedGameIds = initialSet,
            isAnalyzing = false,
            result = null,
            errorMessage = null,
            searchQuery = "",
            filterConsole = GameConsole.ALL,
            filterStatus = "ALL"
        )
    }

    fun closeVersusPicker() {
        versusState.value = versusState.value.copy(isOpen = false)
    }

    fun toggleVersusGame(game: Game) {
        val currentIds = versusState.value.selectedGameIds.toMutableSet()
        if (currentIds.contains(game.id)) {
            currentIds.remove(game.id)
            versusState.value = versusState.value.copy(
                selectedGameIds = currentIds,
                result = null,
                errorMessage = null
            )
        } else {
            if (currentIds.size >= 3) {
                versusState.value = versusState.value.copy(
                    errorMessage = "You can compare up to 3 games at a time."
                )
                return
            }
            currentIds.add(game.id)
            versusState.value = versusState.value.copy(
                selectedGameIds = currentIds,
                result = null,
                errorMessage = null
            )
        }
    }

    fun removeVersusGame(gameId: Long) {
        val currentIds = versusState.value.selectedGameIds.toMutableSet()
        currentIds.remove(gameId)
        versusState.value = versusState.value.copy(
            selectedGameIds = currentIds,
            result = null,
            errorMessage = null
        )
    }

    fun clearVersusSelection() {
        versusState.value = versusState.value.copy(
            selectedGameIds = emptySet(),
            result = null,
            errorMessage = null
        )
    }

    fun onVersusSearchQueryChange(query: String) {
        versusState.value = versusState.value.copy(searchQuery = query)
    }

    fun onVersusConsoleFilterChange(console: GameConsole) {
        versusState.value = versusState.value.copy(filterConsole = console)
    }

    fun onVersusStatusFilterChange(status: String) {
        versusState.value = versusState.value.copy(filterStatus = status)
    }

    fun runVersusComparison() {
        val ids = versusState.value.selectedGameIds
        if (ids.size !in 2..3) {
            versusState.value = versusState.value.copy(
                errorMessage = "Please select 2 or 3 games to compare."
            )
            return
        }

        val all = allGames.value
        val selected = ids.mapNotNull { id -> all.find { it.id == id } }
        if (selected.size !in 2..3) {
            versusState.value = versusState.value.copy(
                errorMessage = "Could not find selected games in your library."
            )
            return
        }

        viewModelScope.launch {
            versusState.value = versusState.value.copy(isAnalyzing = true, errorMessage = null)
            try {
                val comparisonResult = VersusComparisonEngine.compareGames(selected)
                versusState.value = versusState.value.copy(
                    isAnalyzing = false,
                    result = comparisonResult,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val offlineResult = VersusComparisonEngine.evaluateWithOfflineEngine(selected)
                versusState.value = versusState.value.copy(
                    isAnalyzing = false,
                    result = offlineResult,
                    errorMessage = null
                )
            }
        }
    }

    fun resetVersusComparison() {
        versusState.value = versusState.value.copy(
            result = null,
            isAnalyzing = false,
            errorMessage = null
        )
    }

    // ==========================================
    // EXPORT & BACKUP SYSTEM
    // ==========================================

    fun openBackupSheet() {
        isBackupSheetOpen.value = true
        backupOperationState.value = BackupOperationState.Idle
    }

    fun closeBackupSheet() {
        isBackupSheetOpen.value = false
        backupOperationState.value = BackupOperationState.Idle
    }

    fun resetBackupState() {
        backupOperationState.value = BackupOperationState.Idle
    }

    fun exportLibrary(format: ExportFormat) {
        val currentGames = allGames.value
        if (currentGames.isEmpty()) {
            backupOperationState.value = BackupOperationState.Error("Your library is empty. Add games before exporting.")
            return
        }

        viewModelScope.launch {
            backupOperationState.value = BackupOperationState.Exporting(
                format = format,
                current = 0,
                total = currentGames.size,
                message = "Preparing ${format.displayName}..."
            )

            try {
                val timestampStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                val defaultFileName = "the_tavern_backup_$timestampStr.${format.extension}"

                val content = when (format) {
                    ExportFormat.JSON -> {
                        LibraryBackupService.generateJsonExport(
                            games = currentGames,
                            appName = "The Tavern Game Vault",
                            appVersion = "1.0",
                            onProgress = { cur, tot ->
                                backupOperationState.value = BackupOperationState.Exporting(
                                    format = format,
                                    current = cur,
                                    total = tot,
                                    message = "Exporting game $cur of $tot..."
                                )
                            }
                        )
                    }
                    ExportFormat.CSV -> {
                        LibraryBackupService.generateCsvExport(
                            games = currentGames,
                            onProgress = { cur, tot ->
                                backupOperationState.value = BackupOperationState.Exporting(
                                    format = format,
                                    current = cur,
                                    total = tot,
                                    message = "Generating CSV row $cur of $tot..."
                                )
                            }
                        )
                    }
                }

                backupOperationState.value = BackupOperationState.ExportReady(
                    format = format,
                    fileContent = content,
                    defaultFileName = defaultFileName,
                    gamesCount = currentGames.size
                )
            } catch (e: Exception) {
                backupOperationState.value = BackupOperationState.Error(
                    "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun analyzeBackupFromUri(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            backupOperationState.value = BackupOperationState.ImportAnalyzing(0, 0, "Reading backup file...")
            val content = LibraryBackupService.readContentFromUri(context, fileUri)
            if (content == null || content.isBlank()) {
                backupOperationState.value = BackupOperationState.Error("Unable to read the selected backup file. Please check file permissions.")
                return@launch
            }
            analyzeBackupContent(content)
        }
    }

    fun analyzeBackupContent(jsonContent: String) {
        viewModelScope.launch {
            val localGames = allGames.value
            backupOperationState.value = BackupOperationState.ImportAnalyzing(0, localGames.size, "Analyzing backup contents...")

            val analysis = LibraryBackupService.analyzeBackupJson(
                jsonString = jsonContent,
                currentLocalGames = localGames,
                onProgress = { cur, tot ->
                    backupOperationState.value = BackupOperationState.ImportAnalyzing(cur, tot, "Matching records $cur of $tot...")
                }
            )

            if (!analysis.isValid) {
                backupOperationState.value = BackupOperationState.Error(analysis.errorMessage ?: "Malformed or unreadable backup file.")
            } else {
                backupOperationState.value = BackupOperationState.ImportPreview(analysis)
            }
        }
    }

    fun cancelImportPreview() {
        backupOperationState.value = BackupOperationState.Idle
    }

    fun confirmRestore(analysis: ImportAnalysisResult, strategy: ConflictResolutionStrategy) {
        viewModelScope.launch {
            val email = userProfile.value.email
            val totalToProcess = analysis.newGames.size + analysis.existingMatches.size
            backupOperationState.value = BackupOperationState.Importing(0, totalToProcess, "Restoring games...")

            var processedCount = 0
            var newlyAddedCount = 0
            var updatedCount = 0
            var skippedCount = 0

            // 1. Process New Games (items with no conflict in local database)
            for (newGame in analysis.newGames) {
                val gameToInsert = if (email.isNotBlank()) {
                    newGame.copy(id = 0L, userEmail = email)
                } else {
                    newGame.copy(id = 0L)
                }
                val newId = repository.insertGame(gameToInsert)
                if (email.isNotBlank()) {
                    val saved = repository.getGameById(newId) ?: gameToInsert.copy(id = newId)
                    firestoreSyncManager.saveGameToFirestore(email, saved, viewModelScope)
                }
                newlyAddedCount++
                processedCount++
                if (processedCount % 10 == 0 || processedCount == totalToProcess) {
                    backupOperationState.value = BackupOperationState.Importing(
                        current = processedCount,
                        total = totalToProcess,
                        message = "Restored $processedCount of $totalToProcess games..."
                    )
                }
            }

            // 2. Process Existing Games based on Conflict Resolution Strategy
            for (match in analysis.existingMatches) {
                when (strategy) {
                    ConflictResolutionStrategy.KEEP_EXISTING -> {
                        skippedCount++
                    }
                    ConflictResolutionStrategy.REPLACE_EXISTING -> {
                        val replacement = match.backupGame.copy(
                            id = match.localGame.id,
                            userEmail = if (email.isNotBlank()) email else match.localGame.userEmail
                        )
                        repository.updateGame(replacement)
                        if (email.isNotBlank()) {
                            firestoreSyncManager.saveGameToFirestore(email, replacement, viewModelScope)
                        }
                        updatedCount++
                    }
                    ConflictResolutionStrategy.MERGE -> {
                        val merged = LibraryBackupService.resolveGameMerge(match.localGame, match.backupGame)
                        val finalMerged = if (email.isNotBlank()) merged.copy(userEmail = email) else merged
                        repository.updateGame(finalMerged)
                        if (email.isNotBlank()) {
                            firestoreSyncManager.saveGameToFirestore(email, finalMerged, viewModelScope)
                        }
                        updatedCount++
                    }
                }
                processedCount++
                if (processedCount % 10 == 0 || processedCount == totalToProcess) {
                    backupOperationState.value = BackupOperationState.Importing(
                        current = processedCount,
                        total = totalToProcess,
                        message = "Restored $processedCount of $totalToProcess games..."
                    )
                }
            }

            val summaryDetails = buildString {
                append("Added $newlyAddedCount new game(s). ")
                if (updatedCount > 0) append("Updated $updatedCount existing game(s). ")
                if (skippedCount > 0) append("Kept $skippedCount existing game(s) untouched. ")
                if (email.isNotBlank()) append("Synchronized to cloud.")
            }

            backupOperationState.value = BackupOperationState.Completed(
                message = "Restore Completed Successfully!",
                count = newlyAddedCount + updatedCount,
                details = summaryDetails
            )
        }
    }

    fun saveContentToUri(context: Context, uri: Uri, content: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = LibraryBackupService.writeContentToUri(context, uri, content)
            onComplete(success)
        }
    }

    private data class FilterParams(
        val query: String,
        val consoleId: String,
        val genre: String,
        val year: Int,
        val minYear: Int,
        val maxYear: Int,
        val status: String,
        val sortBy: String
    )
}

@Immutable
enum class PlatformOption(
    val id: String,
    val displayName: String,
    val emoji: String,
    val category: String,
    val consoleIds: List<String> = emptyList()
) {
    ALL(
        id = "ALL",
        displayName = "All Platforms",
        emoji = "🎮",
        category = "Overview",
        consoleIds = emptyList()
    ),
    PC(
        id = "PC",
        displayName = "PC",
        emoji = "💻",
        category = "PC",
        consoleIds = listOf("PC")
    ),
    PLAYSTATION(
        id = "PLAYSTATION",
        displayName = "PlayStation",
        emoji = "🎮",
        category = "PlayStation",
        consoleIds = listOf("PS2", "PSP", "PS3", "PS_VITA", "PS4", "PS5")
    ),
    SWITCH(
        id = "SWITCH",
        displayName = "Nintendo Switch",
        emoji = "🔴",
        category = "Nintendo",
        consoleIds = listOf("SWITCH")
    ),
    NINTENDO(
        id = "NINTENDO",
        displayName = "Nintendo (All)",
        emoji = "🕹️",
        category = "Nintendo",
        consoleIds = listOf("N64", "GBA", "GAMECUBE", "NINTENDO_DS", "WII", "NINTENDO_3DS", "WII_U", "SWITCH")
    ),
    XBOX(
        id = "XBOX",
        displayName = "Xbox",
        emoji = "💚",
        category = "Xbox",
        consoleIds = listOf("XBOX", "XBOX_360", "XBOX_ONE")
    );

    fun supports(console: GameConsole): Boolean {
        if (this == ALL) return true
        val tempGame = Game(id = 0, title = "", consoleId = console.id, genre = "")
        return matchesGame(tempGame)
    }

    fun matchesGame(game: Game): Boolean {
        if (this == ALL) return true
        val cId = game.consoleId.trim().uppercase()
        val cCategory = game.console.category.trim().uppercase()
        val cShortName = game.console.shortName.trim().uppercase()
        val cDisplayName = game.console.displayName.trim().uppercase()

        // 1. Direct console ID match from consoleIds list
        if (consoleIds.any { it.equals(cId, ignoreCase = true) }) {
            return true
        }

        // 2. Category match (PlayStation category, Xbox category, Nintendo category, PC category)
        if (category.isNotBlank() && !category.equals("Overview", ignoreCase = true)) {
            if (cCategory.equals(category.uppercase(), ignoreCase = true)) {
                return true
            }
        }

        // 3. Exact specific platform checks
        return when (this) {
            PC -> cId == "PC" || cCategory == "PC" || cShortName == "PC" || cDisplayName.contains("PC")
            XBOX -> cCategory == "XBOX" || cId.startsWith("XBOX") || cDisplayName.contains("XBOX", ignoreCase = true)
            PLAYSTATION -> cCategory == "PLAYSTATION" || cId.startsWith("PS") || cDisplayName.contains("PLAYSTATION", ignoreCase = true)
            SWITCH -> cId == "SWITCH" || cShortName.equals("SWITCH", ignoreCase = true) || cDisplayName.contains("SWITCH", ignoreCase = true)
            NINTENDO -> cCategory == "NINTENDO" || cDisplayName.contains("NINTENDO", ignoreCase = true) || cDisplayName.contains("GAME BOY", ignoreCase = true)
            ALL -> true
        }
    }

    companion object {
        fun fromConsole(console: GameConsole): PlatformOption {
            return when (console) {
                GameConsole.ALL -> ALL
                GameConsole.PC -> PC
                GameConsole.SWITCH -> SWITCH
                GameConsole.PS2, GameConsole.PSP, GameConsole.PS3,
                GameConsole.PS_VITA, GameConsole.PS4, GameConsole.PS5 -> PLAYSTATION
                GameConsole.XBOX, GameConsole.XBOX_360, GameConsole.XBOX_ONE -> XBOX
                GameConsole.N64, GameConsole.GBA, GameConsole.GAMECUBE,
                GameConsole.NINTENDO_DS, GameConsole.WII, GameConsole.NINTENDO_3DS,
                GameConsole.WII_U -> NINTENDO
            }
        }
    }
}

@Immutable
data class RandomSelectionState(
    val isOpen: Boolean = false,
    val selectedPlatform: PlatformOption = PlatformOption.ALL,
    val selectedConsole: GameConsole? = null,
    val selectedStatus: String = "ALL",
    val resultGame: Game? = null,
    val eligibleGamesCount: Int = 0,
    val emptyStateMessage: String? = null,
    val rollCount: Int = 0
)

@Immutable
data class VersusSelectionState(
    val isOpen: Boolean = false,
    val selectedGameIds: Set<Long> = emptySet(),
    val isAnalyzing: Boolean = false,
    val result: VersusResult? = null,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val filterConsole: GameConsole = GameConsole.ALL,
    val filterStatus: String = "ALL"
)

