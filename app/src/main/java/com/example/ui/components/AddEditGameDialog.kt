package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ai.GameAiDetector
import com.example.data.metadata.ExternalGameMetadata
import com.example.data.metadata.ExternalMetadataSearchResult
import com.example.data.metadata.GameCoverFetcher
import com.example.data.metadata.GameMetadataRepository
import com.example.data.metadata.PlatformMapper
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.WishlistStatus
import com.example.data.model.CoverSourceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

internal fun saveImageUriToInternalStorage(context: Context, uri: Uri): String {
    return try {
        val coversDir = File(context.filesDir, "game_covers").apply { mkdirs() }
        val fileName = "cover_${System.currentTimeMillis()}.jpg"
        val destFile = File(coversDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile.toURI().toString()
    } catch (e: Exception) {
        uri.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGameDialog(
    gameToEdit: Game? = null,
    initialConsole: GameConsole = GameConsole.PS2,
    onDismiss: () -> Unit,
    onSave: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by remember { mutableStateOf(gameToEdit?.title ?: "") }
    var selectedConsole by remember {
        mutableStateOf(
            gameToEdit?.console ?: if (initialConsole != GameConsole.ALL) initialConsole else GameConsole.PS2
        )
    }
    var genre by remember { mutableStateOf(gameToEdit?.genre ?: "Action-Adventure") }
    var emulator by remember {
        mutableStateOf(
            gameToEdit?.emulator?.ifBlank { null }
                ?: (gameToEdit?.console ?: (if (initialConsole != GameConsole.ALL) initialConsole else GameConsole.PS2)).primaryEmulator()
        )
    }
    var youtubeLink by remember { mutableStateOf(gameToEdit?.youtubeVideoId ?: "") }
    var coverArtUrl by remember { mutableStateOf(gameToEdit?.coverArtUrl ?: "") }
    var coverThumbUrl by remember { mutableStateOf(gameToEdit?.coverThumbUrl ?: "") }
    var showSteamGridDbModal by remember { mutableStateOf(false) }
    var backCoverUrl by remember { mutableStateOf(gameToEdit?.backCoverUrl ?: "") }
    var completeCaseArtwork by remember { mutableStateOf(gameToEdit?.completeCaseArtwork ?: "") }
    var coverSource by remember { mutableStateOf(gameToEdit?.coverSource ?: "") }
    var backCoverSource by remember { mutableStateOf(gameToEdit?.backCoverSource ?: "") }
    var targetPrice by remember { mutableStateOf(gameToEdit?.targetPrice ?: "$29.99") }
    var selectedStatus by remember { mutableStateOf(gameToEdit?.status ?: WishlistStatus.WANT_TO_PLAY) }
    var rating by remember { mutableStateOf(gameToEdit?.userRating ?: 4.8f) }
    var notes by remember { mutableStateOf(gameToEdit?.notes ?: "") }
    var emulatorNotes by remember { mutableStateOf(gameToEdit?.emulatorNotes ?: "") }
    var recommendedBackend by remember { mutableStateOf(gameToEdit?.recommendedBackend ?: "") }
    var internalResolution by remember { mutableStateOf(gameToEdit?.internalResolution ?: "") }
    var biosRequirement by remember { mutableStateOf(gameToEdit?.biosRequirement ?: "") }

    // External Metadata Fields
    var developer by remember { mutableStateOf(gameToEdit?.developer ?: "") }
    var publisher by remember { mutableStateOf(gameToEdit?.publisher ?: "") }
    var releaseYear by remember { mutableIntStateOf(gameToEdit?.releaseYear ?: 2010) }
    var releaseDate by remember { mutableStateOf(gameToEdit?.releaseDate ?: "") }
    var description by remember { mutableStateOf(gameToEdit?.description ?: "") }
    var bannerArtUrl by remember { mutableStateOf(gameToEdit?.bannerArtUrl ?: "") }
    var websiteUrl by remember { mutableStateOf(gameToEdit?.websiteUrl ?: "") }
    var metacriticScore by remember { mutableIntStateOf(gameToEdit?.metacriticScore ?: 0) }
    var supportedPlatforms by remember { mutableStateOf(gameToEdit?.supportedPlatforms ?: "") }
    var externalProvider by remember { mutableStateOf(gameToEdit?.externalProvider ?: "RAWG") }
    var externalGameId by remember { mutableStateOf(gameToEdit?.externalGameId ?: "") }
    var externalSyncStatus by remember { mutableStateOf(gameToEdit?.externalSyncStatus ?: "") }
    var externalLastUpdated by remember { mutableLongStateOf(gameToEdit?.externalLastUpdated ?: 0L) }

    // External Search Candidates
    var searchCandidates by remember { mutableStateOf<List<ExternalMetadataSearchResult>>(emptyList()) }
    var selectedCandidateId by remember { mutableStateOf<String?>(gameToEdit?.externalGameId?.ifBlank { null }) }
    var isSearchingExternal by remember { mutableStateOf(false) }

    // Jobs and tracking for real-time dynamic search and debounce
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var detailsJob by remember { mutableStateOf<Job?>(null) }
    var lastSearchedQuery by remember { mutableStateOf(gameToEdit?.title?.trim() ?: "") }
    var isApplyingSelectedCandidate by remember { mutableStateOf(false) }

    // Dynamic gradient colors
    var coverGradientStart by remember { mutableStateOf(gameToEdit?.coverGradientStart ?: 0xFF1E1B4BL) }
    var coverGradientEnd by remember { mutableStateOf(gameToEdit?.coverGradientEnd ?: 0xFF0F172AL) }
    var coverAccentColor by remember { mutableStateOf(gameToEdit?.coverAccentColor ?: 0xFF6366F1L) }

    // Detection state
    var isDetecting by remember { mutableStateOf(false) }
    var detectionStatusMessage by remember { mutableStateOf<String?>(null) }
    var detectedBadge by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Photo pickers - Front Cover
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageUriToInternalStorage(context, uri)
            coverArtUrl = savedPath
            coverSource = CoverSourceType.MANUAL
            errorMessage = null
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageUriToInternalStorage(context, uri)
            coverArtUrl = savedPath
            coverSource = CoverSourceType.MANUAL
            errorMessage = null
        }
    }

    fun pickPhotoFromGallery() {
        try {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            getContentLauncher.launch("image/*")
        }
    }

    // Photo pickers - Back Cover
    val backGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageUriToInternalStorage(context, uri)
            backCoverUrl = savedPath
            backCoverSource = CoverSourceType.MANUAL
            errorMessage = null
        }
    }

    val getBackContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageUriToInternalStorage(context, uri)
            backCoverUrl = savedPath
            backCoverSource = CoverSourceType.MANUAL
            errorMessage = null
        }
    }

    fun pickBackPhotoFromGallery() {
        try {
            backGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            getBackContentLauncher.launch("image/*")
        }
    }

    // Photo pickers - Complete Case Insert (Front + Spine + Back)
    val completeCaseGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageUriToInternalStorage(context, uri)
            completeCaseArtwork = savedPath
            coverSource = CoverSourceType.MANUAL
            errorMessage = null
        }
    }

    val getCompleteCaseContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageUriToInternalStorage(context, uri)
            completeCaseArtwork = savedPath
            coverSource = CoverSourceType.MANUAL
            errorMessage = null
        }
    }

    fun pickCompleteCaseFromGallery() {
        try {
            completeCaseGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            getCompleteCaseContentLauncher.launch("image/*")
        }
    }

    suspend fun applyExternalMetadata(meta: ExternalGameMetadata) {
        title = meta.title.ifBlank { title }
        if (meta.suggestedConsole != null && selectedConsole == GameConsole.PS2) {
            selectedConsole = meta.suggestedConsole
        }
        if (meta.genres.isNotEmpty()) {
            genre = meta.genres.first()
        }
        if (meta.coverArtUrl.isNotBlank()) {
            coverArtUrl = meta.coverArtUrl
            coverSource = CoverSourceType.EXISTING
        }
        if (meta.bannerArtUrl.isNotBlank()) {
            bannerArtUrl = meta.bannerArtUrl
        }
        if (meta.publishers.isNotEmpty()) {
            publisher = meta.publishers.joinToString(", ")
        }
        if (meta.developers.isNotEmpty()) {
            developer = meta.developers.joinToString(", ")
        }
        if (meta.releaseDate.isNotBlank()) {
            releaseDate = meta.releaseDate
        }
        if (meta.releaseYear > 0) {
            releaseYear = meta.releaseYear
        }
        if (meta.description.isNotBlank()) {
            description = meta.description
            if (notes.isBlank()) {
                notes = meta.description
            }
        }
        if (meta.websiteUrl.isNotBlank()) {
            websiteUrl = meta.websiteUrl
        }
        if (meta.metacriticScore > 0) {
            metacriticScore = meta.metacriticScore
        }
        if (meta.platformsDisplay.isNotBlank()) {
            supportedPlatforms = meta.platformsDisplay
        }
        externalProvider = meta.externalProvider
        externalGameId = meta.externalGameId
        externalSyncStatus = "SYNCED"
        externalLastUpdated = System.currentTimeMillis()
        selectedCandidateId = meta.externalGameId

        // Resolve cover details if backCover, completeCaseArtwork, or frontCover are missing
        val resolved = GameCoverFetcher.resolveCoverWithDetails(
            title = meta.title.ifBlank { title },
            console = selectedConsole,
            releaseYear = releaseYear,
            existingCoverUrl = coverArtUrl,
            existingCoverSource = coverSource,
            existingBackCoverUrl = backCoverUrl,
            existingCompleteCaseArtwork = completeCaseArtwork
        )
        if (coverArtUrl.isBlank()) {
            coverArtUrl = resolved.coverUrl
            coverThumbUrl = resolved.thumbUrl
            coverSource = resolved.coverSource
        }
        if (backCoverUrl.isBlank()) {
            backCoverUrl = resolved.backCoverUrl
            backCoverSource = resolved.backCoverSource
        }
        if (completeCaseArtwork.isBlank() && resolved.completeCaseArtwork.isNotBlank()) {
            completeCaseArtwork = resolved.completeCaseArtwork
        }

        // Update color scheme from console
        coverGradientStart = selectedConsole.brandColorHex
        coverAccentColor = selectedConsole.accentColorHex

        // Emulator
        if (emulator.isBlank() || emulator in GameConsole.entries.flatMap { it.defaultEmulators() }) {
            emulator = selectedConsole.primaryEmulator()
        }
    }

    fun selectExternalCandidate(candidate: ExternalMetadataSearchResult) {
        selectedCandidateId = candidate.externalGameId
        isDetecting = true
        detectionStatusMessage = "Fetching full metadata for '${candidate.title}'..."

        detailsJob?.cancel()
        detailsJob = coroutineScope.launch {
            try {
                val detailsResult = GameMetadataRepository.fetchGameDetails(candidate.externalGameId, selectedConsole)
                if (!isActive) return@launch

                detailsResult.onSuccess { metadata ->
                    if (!isActive) return@launch
                    isApplyingSelectedCandidate = true
                    applyExternalMetadata(metadata)
                    lastSearchedQuery = metadata.title.trim()
                    detectedBadge = true
                    detectionStatusMessage = "Auto-filled from external database: ${metadata.title}"
                }.onFailure {
                    if (!isActive) return@launch
                    isApplyingSelectedCandidate = true
                    title = candidate.title
                    lastSearchedQuery = candidate.title.trim()
                    val resolved = GameCoverFetcher.resolveCoverWithDetails(
                        title = candidate.title,
                        console = selectedConsole,
                        releaseYear = candidate.releaseYear,
                        existingCoverUrl = candidate.coverArtUrl.ifBlank { coverArtUrl },
                        existingCoverSource = coverSource,
                        existingBackCoverUrl = backCoverUrl,
                        existingCompleteCaseArtwork = completeCaseArtwork
                    )
                    coverArtUrl = resolved.coverUrl
                    coverThumbUrl = resolved.thumbUrl
                    coverSource = resolved.coverSource
                    if (backCoverUrl.isBlank()) {
                        backCoverUrl = resolved.backCoverUrl
                        backCoverSource = resolved.backCoverSource
                    }
                    if (completeCaseArtwork.isBlank() && resolved.completeCaseArtwork.isNotBlank()) {
                        completeCaseArtwork = resolved.completeCaseArtwork
                    }
                    if (candidate.releaseYear > 0) releaseYear = candidate.releaseYear
                    if (candidate.genreDisplay.isNotBlank()) genre = candidate.genreDisplay
                    if (candidate.metacriticScore > 0) metacriticScore = candidate.metacriticScore
                    if (candidate.platformsSummary.isNotBlank()) supportedPlatforms = candidate.platformsSummary
                    externalGameId = candidate.externalGameId
                    externalProvider = "RAWG"
                    externalSyncStatus = "SYNCED"
                    detectedBadge = true
                    detectionStatusMessage = "Selected: ${candidate.title}"
                }
            } finally {
                if (isActive) {
                    isDetecting = false
                }
            }
        }
    }

    fun runGameDetection() {
        val query = title.trim()
        if (query.isBlank()) {
            errorMessage = "Please enter a game name to search."
            return
        }
        errorMessage = null
        keyboardController?.hide()

        searchJob?.cancel()
        detailsJob?.cancel()

        isDetecting = true
        searchCandidates = emptyList()
        selectedCandidateId = null
        detectedBadge = false
        detectionStatusMessage = "Searching external database (RAWG) & cover catalog for '$query'..."

        searchJob = coroutineScope.launch {
            try {
                val searchResult = GameMetadataRepository.searchGames(query, selectedConsole)
                if (!isActive) return@launch
                val candidates = searchResult.getOrDefault(emptyList())

                if (title.trim() == query) {
                    searchCandidates = candidates
                    lastSearchedQuery = query

                    if (candidates.isNotEmpty()) {
                        val first = candidates.first()
                        selectExternalCandidate(first)
                    } else {
                        val fallback = GameAiDetector.findInCatalogOrSynthesize(query)
                        isApplyingSelectedCandidate = true
                        title = fallback.title
                        selectedConsole = fallback.console
                        genre = fallback.genre
                        emulator = fallback.emulator.ifBlank { fallback.console.primaryEmulator() }
                        youtubeLink = fallback.youtubeVideoId
                        val resolved = GameCoverFetcher.resolveCoverWithDetails(
                            title = fallback.title,
                            console = fallback.console,
                            releaseYear = fallback.releaseYear,
                            existingCoverUrl = coverArtUrl,
                            existingBackCoverUrl = backCoverUrl
                        )
                        coverArtUrl = resolved.coverUrl.ifBlank { fallback.coverArtUrl }
                        coverSource = resolved.coverSource
                        backCoverUrl = resolved.backCoverUrl.ifBlank { fallback.backCoverUrl }
                        backCoverSource = resolved.backCoverSource
                        if (fallback.emulatorNotes.isNotBlank()) emulatorNotes = fallback.emulatorNotes
                        if (fallback.recommendedBackend.isNotBlank()) recommendedBackend = fallback.recommendedBackend
                        if (fallback.internalResolution.isNotBlank()) internalResolution = fallback.internalResolution
                        if (fallback.biosRequirement.isNotBlank()) biosRequirement = fallback.biosRequirement
                        detectedBadge = true
                        detectionStatusMessage = "Auto-filled: ${fallback.title} (${fallback.console.displayName})"
                        isDetecting = false
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isActive) return@launch
                isDetecting = false
                detectionStatusMessage = "Search error: ${e.message}"
            }
        }
    }

    // Debounce & Real-time Synchronization for Search Query
    LaunchedEffect(title, selectedConsole) {
        val cleanQuery = title.trim()

        if (isApplyingSelectedCandidate) {
            isApplyingSelectedCandidate = false
            return@LaunchedEffect
        }

        if (cleanQuery.length < 2) {
            searchCandidates = emptyList()
            selectedCandidateId = null
            isDetecting = false
            return@LaunchedEffect
        }

        if (cleanQuery.equals(lastSearchedQuery, ignoreCase = true) && searchCandidates.isNotEmpty()) {
            return@LaunchedEffect
        }

        // Cancel previous background jobs immediately
        searchJob?.cancel()
        detailsJob?.cancel()

        // Reset previous results & set searching state
        searchCandidates = emptyList()
        selectedCandidateId = null
        detectedBadge = false
        isDetecting = true
        detectionStatusMessage = "Searching external database for '$cleanQuery'..."

        // Debounce 400ms - cancelled automatically if user continues typing
        delay(400)

        try {
            val searchResult = GameMetadataRepository.searchGames(cleanQuery, selectedConsole)
            if (!isActive) return@LaunchedEffect

            val candidates = searchResult.getOrDefault(emptyList())
            if (title.trim() == cleanQuery) {
                searchCandidates = candidates
                lastSearchedQuery = cleanQuery
                isDetecting = false
                if (candidates.isNotEmpty()) {
                    detectionStatusMessage = "Found ${candidates.size} matching results for '$cleanQuery'"
                } else {
                    detectionStatusMessage = "No external matches found for '$cleanQuery'"
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isActive) return@LaunchedEffect
            if (title.trim() == cleanQuery) {
                isDetecting = false
                detectionStatusMessage = "Search error: ${e.message}"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1B24),
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false
        ),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF49454F))
            )
        },
        modifier = modifier.testTag("add_edit_game_sheet")
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (gameToEdit == null) "Add Game to Wishlist" else "Edit Game Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF5EEF8)
                        )
                    )
                    Text(
                        text = "Automatically finds console, cover art & trailer.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC7B8E0))
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFCAC4D0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Name Input & Search Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF282236),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "GAME SEARCH",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0BCFF),
                                letterSpacing = 1.2.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title Input with dynamic reset, in-flight cancellation, and clear action
                    OutlinedTextField(
                        value = title,
                        onValueChange = { newTitle ->
                            title = newTitle
                            errorMessage = null

                            // Cancel any running jobs immediately
                            searchJob?.cancel()
                            detailsJob?.cancel()

                            // Immediately reset stale search results and auto-fill state
                            searchCandidates = emptyList()
                            selectedCandidateId = null
                            detectedBadge = false
                            detectionStatusMessage = null

                            // If adding a new game and artwork was not manually chosen, clear old auto-filled art
                            if (gameToEdit == null && coverSource != CoverSourceType.MANUAL) {
                                coverArtUrl = ""
                                coverThumbUrl = ""
                                coverSource = ""
                                backCoverUrl = ""
                                backCoverSource = ""
                                completeCaseArtwork = ""
                                youtubeLink = ""
                            }

                            if (newTitle.trim().length >= 2) {
                                isDetecting = true
                                detectionStatusMessage = "Searching for '${newTitle.trim()}'..."
                            } else {
                                isDetecting = false
                            }
                        },
                        label = { Text("Game Name *") },
                        placeholder = { Text("e.g. Silent Hill 2, Crisis Core, Persona 4...") },
                        singleLine = true,
                        trailingIcon = {
                            if (title.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        title = ""
                                        searchJob?.cancel()
                                        detailsJob?.cancel()
                                        searchCandidates = emptyList()
                                        selectedCandidateId = null
                                        detectedBadge = false
                                        detectionStatusMessage = null
                                        isDetecting = false
                                        if (gameToEdit == null && coverSource != CoverSourceType.MANUAL) {
                                            coverArtUrl = ""
                                            coverThumbUrl = ""
                                            coverSource = ""
                                            backCoverUrl = ""
                                            backCoverSource = ""
                                            completeCaseArtwork = ""
                                            youtubeLink = ""
                                        }
                                    },
                                    modifier = Modifier.testTag("clear_game_title_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear game name",
                                        tint = Color(0xFFCAC4D0),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { runGameDetection() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_game_title"),
                        colors = outlinedTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Button
                    Button(
                        onClick = { runGameDetection() },
                        enabled = !isDetecting && title.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("ai_detect_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF382E4D),
                            disabledContentColor = Color(0xFF8B829C)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isDetecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFFD0BCFF),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Finding Details...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (detectedBadge) "Re-Search Game Info" else "Find Game Info & Cover",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dedicated Găsește copertă (SteamGridDB v2) Button
                    Button(
                        onClick = { showSteamGridDbModal = true },
                        enabled = title.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_find_cover_steamgriddb_header"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1B1829),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (coverArtUrl.isBlank()) "Găsește copertă (SteamGridDB)" else "Schimbă coperta (SteamGridDB)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Detection Status feedback
                    if (detectionStatusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF1E172B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (detectedBadge) Icons.Default.Check else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (detectedBadge) Color(0xFF4ADE80) else Color(0xFFD0BCFF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = detectionStatusMessage!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (detectedBadge) Color(0xFF86EFAC) else Color(0xFFD0BCFF),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // Candidate results from external API (dynamically updated in real-time)
                    if (searchCandidates.isNotEmpty() || (isDetecting && title.trim().length >= 2)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ExternalMetadataCandidateSelector(
                            candidates = searchCandidates,
                            selectedGameId = selectedCandidateId,
                            isLoading = isDetecting && searchCandidates.isEmpty(),
                            onSelectCandidate = { candidate ->
                                selectExternalCandidate(candidate)
                            }
                        )
                    }
                    if (detectedBadge && (coverArtUrl.isNotBlank() || title.isNotBlank())) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFF191324),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedConsole.accentColorHex).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Cover thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(54.dp, 72.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(coverGradientStart), Color(coverGradientEnd))
                                            )
                                        )
                                ) {
                                    if (coverArtUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = coverArtUrl,
                                            contentDescription = "Cover preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = Color(selectedConsole.brandColorHex).copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${selectedConsole.emoji} ${selectedConsole.displayName}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "🎬 Trailer ID: ${youtubeLink.ifBlank { "Auto-selected" }}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF38BDF8)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Console Selector
            Text(
                text = "CONSOLE (TAP TO CHANGE)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCAC4D0),
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameConsole.selectableConsoles().forEach { console ->
                    val isSelected = selectedConsole == console

                    Surface(
                        modifier = Modifier
                            .testTag("dialog_console_${console.id}")
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedConsole = console
                                coverGradientStart = console.brandColorHex
                                coverAccentColor = console.accentColorHex
                                if (emulator.isBlank() || emulator in GameConsole.entries.flatMap { it.defaultEmulators() }) {
                                    emulator = console.primaryEmulator()
                                }
                            },
                        color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF2B2930),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = console.emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = console.shortName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF381E72) else Color(0xFFCAC4D0)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // YouTube Trailer Link / ID
            OutlinedTextField(
                value = youtubeLink,
                onValueChange = { youtubeLink = it },
                label = { Text("YouTube Trailer (URL or Video ID) *") },
                placeholder = { Text("e.g. dQp8x9Wn1qE or https://youtu.be/...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_youtube_id"),
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CASE COVER ARTWORK: FRONT & BACK 360° MANAGEMENT
            // ==========================================
            var selectedCoverFace by remember { mutableIntStateOf(if (gameToEdit?.hasCompleteCaseArtwork == true) 2 else 0) } // 0 = Front, 1 = Back, 2 = Continuous Full Wrap
            var showFrontUrlInput by remember { mutableStateOf(false) }
            var showBackUrlInput by remember { mutableStateOf(false) }
            var showCompleteCaseUrlInput by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF252033))
                    .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Physical Case Art",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "3D PHYSICAL CASE ARTWORK",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0BCFF),
                                letterSpacing = 1.1.sp
                            )
                        )
                    }

                    Surface(
                        color = Color(0xFF381E72),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFD0BCFF))
                    ) {
                        Text(
                            text = "360° Case",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEADDFF),
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle tabs between Front Cover and Back Cover
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B1726))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Front Face Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCoverFace = 0 },
                        color = if (selectedCoverFace == 0) Color(0xFF6750A4) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎴 Front Cover",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedCoverFace == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedCoverFace == 0) Color.White else Color(0xFFCAC4D0),
                                    fontSize = 12.sp
                                )
                            )
                            if (coverArtUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80))
                                )
                            }
                        }
                    }

                    // Back Face Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCoverFace = 1 },
                        color = if (selectedCoverFace == 1) Color(0xFF6750A4) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔄 Back",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedCoverFace == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedCoverFace == 1) Color.White else Color(0xFFCAC4D0),
                                    fontSize = 12.sp
                                )
                            )
                            if (backCoverUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80))
                                )
                            }
                        }
                    }

                    // Complete Insert Tab (Continuous Wrap)
                    Surface(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCoverFace = 2 },
                        color = if (selectedCoverFace == 2) Color(0xFF6750A4) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📜 Full Wrap",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedCoverFace == 2) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedCoverFace == 2) Color.White else Color(0xFFCAC4D0),
                                    fontSize = 12.sp
                                )
                            )
                            if (completeCaseArtwork.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FRONT COVER CONTENT
                if (selectedCoverFace == 0) {
                    if (coverArtUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF191624), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF383444), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp, 88.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(coverGradientStart), Color(coverGradientEnd))
                                        )
                                    )
                            ) {
                                AsyncImage(
                                    model = coverArtUrl,
                                    contentDescription = "Front Cover Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Front Artwork Loaded",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = when {
                                        coverArtUrl.startsWith("file:") || coverArtUrl.startsWith("content:") -> "Imported from device gallery"
                                        coverSource == CoverSourceType.COVER_PROJECT -> "The Cover Project catalog"
                                        coverSource == CoverSourceType.EXISTING -> "Web database (RAWG/Libretro)"
                                        else -> "Loaded from web art"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFCAC4D0),
                                        fontSize = 11.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { showSteamGridDbModal = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E3A8A),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "SteamGridDB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { pickPhotoFromGallery() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF6750A4),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coverArtUrl = ""
                                            coverThumbUrl = ""
                                            coverSource = ""
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFF87171)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Photo",
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { showSteamGridDbModal = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_find_cover_steamgriddb_front"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Găsește copertă (SteamGridDB)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { pickPhotoFromGallery() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("import_gallery_photo_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF382E4D),
                                contentColor = Color(0xFFEADDFF)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFD0BCFF)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Import Front Cover from Gallery",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showFrontUrlInput = !showFrontUrlInput }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF938F99),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (showFrontUrlInput) "Hide URL input" else "Or paste a front cover URL instead...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFC7B8E0),
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (showFrontUrlInput) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = coverArtUrl,
                            onValueChange = {
                                coverArtUrl = it
                                coverSource = if (it.isNotBlank()) CoverSourceType.EXISTING else ""
                            },
                            label = { Text("Front Cover Image URL") },
                            placeholder = { Text("https://... or file://...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_cover_art_url"),
                            colors = outlinedTextFieldColors()
                        )
                    }
                } else if (selectedCoverFace == 1) {
                    // BACK COVER CONTENT
                    if (backCoverUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF191624), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF383444), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp, 88.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(coverGradientStart), Color(coverGradientEnd))
                                        )
                                    )
                            ) {
                                AsyncImage(
                                    model = backCoverUrl,
                                    contentDescription = "Back Cover Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Back Artwork Loaded",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = when {
                                        backCoverUrl.startsWith("file:") || backCoverUrl.startsWith("content:") -> "Imported from device gallery"
                                        backCoverSource == CoverSourceType.COVER_PROJECT -> "The Cover Project catalog"
                                        backCoverSource == CoverSourceType.EXISTING -> "Web database (RAWG/Libretro)"
                                        else -> "Loaded from web art"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFCAC4D0),
                                        fontSize = 11.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { pickBackPhotoFromGallery() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF6750A4),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            backCoverUrl = ""
                                            backCoverSource = ""
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFF87171)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Back Photo",
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { pickBackPhotoFromGallery() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("import_back_gallery_photo_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F378B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFD0BCFF)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Import Back Cover from Gallery",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showBackUrlInput = !showBackUrlInput }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF938F99),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (showBackUrlInput) "Hide URL input" else "Or paste a back cover URL instead...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFC7B8E0),
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (showBackUrlInput) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = backCoverUrl,
                            onValueChange = {
                                backCoverUrl = it
                                backCoverSource = if (it.isNotBlank()) CoverSourceType.EXISTING else ""
                            },
                            label = { Text("Back Cover Image URL") },
                            placeholder = { Text("https://... or file://...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_back_cover_art_url"),
                            colors = outlinedTextFieldColors()
                        )
                    }
                } else {
                    // COMPLETE PHYSICAL CASE INSERT (FRONT + SPINE + BACK)
                    if (completeCaseArtwork.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF191624), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF383444), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F0D15))
                            ) {
                                AsyncImage(
                                    model = completeCaseArtwork,
                                    contentDescription = "Full Physical Insert (Front, Spine, Back)",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Complete Insert Loaded",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = when {
                                            completeCaseArtwork.startsWith("file:") || completeCaseArtwork.startsWith("content:") -> "Imported from device gallery"
                                            completeCaseArtwork.startsWith("android.resource:") -> "The Cover Project verified scan"
                                            completeCaseArtwork.contains("gametdb.com") -> "GameTDB HQ full insert wrap"
                                            else -> "Continuous physical case wrap"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFCAC4D0),
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { pickCompleteCaseFromGallery() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF6750A4),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            completeCaseArtwork = ""
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFF87171)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(text = "Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty state for full insert
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { pickCompleteCaseFromGallery() }
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF6750A4).copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = Color(0xFF191624),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Import Complete Physical Insert (Wrap)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEADDFF)
                                    )
                                )
                                Text(
                                    text = "Continuous Front + Spine + Back artwork from The Cover Project or personal scan. Textures the 3D case realistically without seams.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFCAC4D0),
                                        fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showCompleteCaseUrlInput = !showCompleteCaseUrlInput }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF938F99),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (showCompleteCaseUrlInput) "Hide URL input" else "Or paste complete insert wrap URL instead...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFC7B8E0),
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (showCompleteCaseUrlInput) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = completeCaseArtwork,
                            onValueChange = {
                                completeCaseArtwork = it
                            },
                            label = { Text("Complete Insert Artwork URL") },
                            placeholder = { Text("https://... or file://...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_complete_case_artwork_url"),
                            colors = outlinedTextFieldColors()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Emulator Category
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "EMULATOR / RUNNER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCAC4D0),
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = emulator,
                    onValueChange = { emulator = it },
                    label = { Text("Emulator *") },
                    placeholder = { Text("e.g. PCSX2, Dolphin, PPSSPP, Vita3K, Ryujinx...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_emulator"),
                    colors = outlinedTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Recommended emulator chips for current console
                val suggestedEmulators = selectedConsole.defaultEmulators()
                if (suggestedEmulators.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedEmulators.forEach { emuName ->
                            val isMatch = emulator.equals(emuName, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { emulator = emuName }
                                    .testTag("chip_emu_$emuName"),
                                color = if (isMatch) Color(0xFFD0BCFF) else Color(0xFF2B2930),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isMatch) Color(0xFFD0BCFF) else Color(0xFF49454F)
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "🎮 $emuName",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isMatch) Color(0xFF381E72) else Color(0xFFE6E1E5)
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFEFB8C8),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Please enter a game name."
                        return@Button
                    }
                    val videoId = extractYoutubeVideoId(youtubeLink).ifBlank {
                        GameAiDetector.getCuratedTrailerForGame(title, selectedConsole)
                    }

                    val finalEmulator = emulator.ifBlank { selectedConsole.primaryEmulator() }.trim()

                    val newOrUpdatedGame = (gameToEdit ?: Game(
                        title = title.trim(),
                        consoleId = selectedConsole.id,
                        genre = if (genre.isBlank()) "Action-Adventure" else genre.trim(),
                        emulator = finalEmulator,
                        youtubeVideoId = videoId,
                        coverArtUrl = coverArtUrl.trim(),
                        coverThumbUrl = coverThumbUrl.trim(),
                        backCoverUrl = backCoverUrl.trim(),
                        completeCaseArtwork = completeCaseArtwork.trim(),
                        coverSource = when {
                            completeCaseArtwork.startsWith("file:") || completeCaseArtwork.startsWith("content:") ||
                            coverArtUrl.startsWith("file:") || coverArtUrl.startsWith("content:") -> CoverSourceType.MANUAL
                            coverSource.isNotBlank() -> coverSource
                            coverArtUrl.isNotBlank() || completeCaseArtwork.isNotBlank() -> CoverSourceType.EXISTING
                            else -> ""
                        },
                        backCoverSource = when {
                            backCoverUrl.startsWith("file:") || backCoverUrl.startsWith("content:") -> CoverSourceType.MANUAL
                            backCoverSource.isNotBlank() -> backCoverSource
                            backCoverUrl.isNotBlank() -> CoverSourceType.EXISTING
                            else -> ""
                        },
                        coverGradientStart = coverGradientStart,
                        coverGradientEnd = coverGradientEnd,
                        coverAccentColor = coverAccentColor,
                        wishlistStatus = selectedStatus.id,
                        userRating = (rating * 10).toInt() / 10f,
                        targetPrice = if (targetPrice.isBlank()) "$29.99" else targetPrice.trim(),
                        notes = notes.trim(),
                        emulatorNotes = emulatorNotes.trim(),
                        recommendedBackend = recommendedBackend.trim(),
                        internalResolution = internalResolution.trim(),
                        biosRequirement = biosRequirement.trim(),
                        developer = developer.trim(),
                        publisher = publisher.trim(),
                        releaseYear = releaseYear,
                        releaseDate = releaseDate.trim(),
                        description = description.trim(),
                        bannerArtUrl = bannerArtUrl.trim(),
                        websiteUrl = websiteUrl.trim(),
                        metacriticScore = metacriticScore,
                        supportedPlatforms = supportedPlatforms.trim(),
                        externalProvider = externalProvider,
                        externalGameId = externalGameId,
                        externalLastUpdated = externalLastUpdated,
                        externalSyncStatus = externalSyncStatus
                    )).copy(
                        title = title.trim(),
                        consoleId = selectedConsole.id,
                        genre = if (genre.isBlank()) "Action-Adventure" else genre.trim(),
                        emulator = finalEmulator,
                        youtubeVideoId = videoId,
                        coverArtUrl = coverArtUrl.trim(),
                        coverThumbUrl = coverThumbUrl.trim(),
                        backCoverUrl = backCoverUrl.trim(),
                        completeCaseArtwork = completeCaseArtwork.trim(),
                        coverSource = when {
                            completeCaseArtwork.startsWith("file:") || completeCaseArtwork.startsWith("content:") ||
                            coverArtUrl.startsWith("file:") || coverArtUrl.startsWith("content:") -> CoverSourceType.MANUAL
                            coverSource.isNotBlank() -> coverSource
                            coverArtUrl.isNotBlank() || completeCaseArtwork.isNotBlank() -> CoverSourceType.EXISTING
                            else -> ""
                        },
                        backCoverSource = when {
                            backCoverUrl.startsWith("file:") || backCoverUrl.startsWith("content:") -> CoverSourceType.MANUAL
                            backCoverSource.isNotBlank() -> backCoverSource
                            backCoverUrl.isNotBlank() -> CoverSourceType.EXISTING
                            else -> ""
                        },
                        coverGradientStart = coverGradientStart,
                        coverGradientEnd = coverGradientEnd,
                        coverAccentColor = coverAccentColor,
                        wishlistStatus = selectedStatus.id,
                        userRating = (rating * 10).toInt() / 10f,
                        targetPrice = if (targetPrice.isBlank()) "$29.99" else targetPrice.trim(),
                        notes = notes.trim(),
                        emulatorNotes = emulatorNotes.trim(),
                        recommendedBackend = recommendedBackend.trim(),
                        internalResolution = internalResolution.trim(),
                        biosRequirement = biosRequirement.trim(),
                        developer = developer.trim(),
                        publisher = publisher.trim(),
                        releaseYear = releaseYear,
                        releaseDate = releaseDate.trim(),
                        description = description.trim(),
                        bannerArtUrl = bannerArtUrl.trim(),
                        websiteUrl = websiteUrl.trim(),
                        metacriticScore = metacriticScore,
                        supportedPlatforms = supportedPlatforms.trim(),
                        externalProvider = externalProvider,
                        externalGameId = externalGameId,
                        externalLastUpdated = externalLastUpdated,
                        externalSyncStatus = externalSyncStatus
                    )

                    onSave(newOrUpdatedGame)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_game_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF381E72))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (gameToEdit == null) "Add to Wishlist Vault" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF381E72)
                )
            }
        }
    }

    if (showSteamGridDbModal) {
        SteamGridDbCoverPickerModal(
            initialTitle = title,
            onDismiss = { showSteamGridDbModal = false },
            onCoverSelected = { fullUrl, thumbUrl ->
                coverArtUrl = fullUrl
                coverThumbUrl = thumbUrl
                coverSource = CoverSourceType.STEAMGRIDDB
                showSteamGridDbModal = false
            }
        )
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFFD0BCFF),
    unfocusedBorderColor = Color(0xFF49454F),
    focusedLabelColor = Color(0xFFD0BCFF),
    unfocusedLabelColor = Color(0xFF938F99),
    cursorColor = Color(0xFFD0BCFF),
    focusedTextColor = Color(0xFFE6E1E5),
    unfocusedTextColor = Color(0xFFCAC4D0),
    focusedContainerColor = Color(0xFF2B2930),
    unfocusedContainerColor = Color(0xFF2B2930)
)
