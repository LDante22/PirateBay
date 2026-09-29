package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.steamgriddb.SteamGridDbGame
import com.example.data.steamgriddb.SteamGridDbGrid
import com.example.data.steamgriddb.SteamGridDbResult
import com.example.data.steamgriddb.SteamGridDbServerEndpoint
import com.example.ui.components.shelf.ShelfAsyncImage
import kotlinx.coroutines.launch

/**
 * SteamGridDB Cover Selection Modal.
 *
 * Implements:
 * 1. Autocomplete game search with title sanitization fallback.
 * 2. Ambiguity resolution: displays up to 5 game matches if multiple found.
 * 3. Fetches 600x900 static vertical posters, ranked by score.
 * 4. Thumbnails in list/selection, full URL persisted with game.
 * 5. Placeholder + manual alternate search or direct image URL paste if not found.
 * 6. Specific error messaging for 401, 429, and network failures with retry affordance.
 * 7. Non-blocking: users can close or dismiss without interrupting game save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteamGridDbCoverPickerModal(
    initialTitle: String,
    onDismiss: () -> Unit,
    onCoverSelected: (fullUrl: String, thumbUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf(initialTitle) }
    var manualAlternateTitle by remember { mutableStateOf("") }
    var manualCustomUrl by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var is401AuthError by remember { mutableStateOf(false) }

    var gameCandidates by remember { mutableStateOf<List<SteamGridDbGame>>(emptyList()) }
    var selectedGameCandidate by remember { mutableStateOf<SteamGridDbGame?>(null) }
    var gridVariants by remember { mutableStateOf<List<SteamGridDbGrid>>(emptyList()) }
    var noResultsFound by remember { mutableStateOf(false) }

    fun searchForGames(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        isLoading = true
        errorMessage = null
        is401AuthError = false
        noResultsFound = false
        gameCandidates = emptyList()
        selectedGameCandidate = null
        gridVariants = emptyList()

        coroutineScope.launch {
            when (val result = SteamGridDbServerEndpoint.searchAutocomplete(q)) {
                is SteamGridDbResult.Success -> {
                    val candidates = result.data.take(5)
                    if (candidates.isEmpty()) {
                        noResultsFound = true
                    } else if (candidates.size == 1) {
                        val candidate = candidates.first()
                        selectedGameCandidate = candidate
                        // Fetch grids for this single candidate
                        when (val gridsResult = SteamGridDbServerEndpoint.fetchGridsForGame(candidate.id)) {
                            is SteamGridDbResult.Success -> {
                                gridVariants = gridsResult.data
                                if (gridVariants.isEmpty()) {
                                    noResultsFound = true
                                }
                            }
                            is SteamGridDbResult.Error -> {
                                errorMessage = gridsResult.message
                                is401AuthError = gridsResult.statusCode == 401
                            }
                            is SteamGridDbResult.NotFound -> {
                                noResultsFound = true
                            }
                            is SteamGridDbResult.AmbiguousMatches -> {
                                noResultsFound = true
                            }
                        }
                    } else {
                        // Ambiguous: show up to 5 candidates for user selection
                        gameCandidates = candidates
                    }
                }
                is SteamGridDbResult.Error -> {
                    errorMessage = result.message
                    is401AuthError = result.statusCode == 401
                }
                is SteamGridDbResult.NotFound -> {
                    noResultsFound = true
                }
                is SteamGridDbResult.AmbiguousMatches -> {
                    gameCandidates = result.games.take(5)
                }
            }
            isLoading = false
        }
    }

    fun selectCandidateAndFetchGrids(candidate: SteamGridDbGame) {
        selectedGameCandidate = candidate
        isLoading = true
        errorMessage = null
        is401AuthError = false
        noResultsFound = false
        gridVariants = emptyList()

        coroutineScope.launch {
            when (val gridsResult = SteamGridDbServerEndpoint.fetchGridsForGame(candidate.id)) {
                is SteamGridDbResult.Success -> {
                    gridVariants = gridsResult.data
                    if (gridVariants.isEmpty()) {
                        noResultsFound = true
                    }
                }
                is SteamGridDbResult.Error -> {
                    errorMessage = gridsResult.message
                    is401AuthError = gridsResult.statusCode == 401
                }
                is SteamGridDbResult.NotFound -> {
                    noResultsFound = true
                }
                is SteamGridDbResult.AmbiguousMatches -> {
                    noResultsFound = true
                }
            }
            isLoading = false
        }
    }

    // Run initial search automatically if title is present
    LaunchedEffect(initialTitle) {
        if (initialTitle.isNotBlank()) {
            searchForGames(initialTitle)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14121E),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SteamGridDB Coperți",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Format vertical 2:3 (600x900) • Calitate oficială",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_steamgriddb_modal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Închide",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar for finding or refining
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Caută joc pe SteamGridDB...", color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_steamgriddb_search"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E1B2E),
                        unfocusedContainerColor = Color(0xFF1E1B2E)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = { searchForGames(searchQuery) },
                    enabled = !isLoading && searchQuery.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_search_steamgriddb")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Caută",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message (401, 429, or Network)
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (is401AuthError) Color(0xFF451A03) else Color(0xFF3B1219),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (is401AuthError) Color(0xFFF59E0B) else Color(0xFFEF4444))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (is401AuthError) Color(0xFFFBBF24) else Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = errorMessage ?: "A apărut o eroare",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        if (!is401AuthError) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { searchForGames(searchQuery) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF60A5FA)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reîncearcă", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ambiguous Titles: Display up to 5 games for user selection
            if (gameCandidates.isNotEmpty() && selectedGameCandidate == null) {
                Text(
                    text = "Am găsit mai multe jocuri. Selectează-l pe cel dorit:",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gameCandidates, key = { it.id }) { candidate ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectCandidateAndFetchGrids(candidate) }
                                .testTag("candidate_item_${candidate.id}"),
                            color = Color(0xFF1E1B2E),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (candidate.releaseDate > 0L) {
                                        val year = try {
                                            java.time.Instant.ofEpochSecond(candidate.releaseDate)
                                                .atZone(java.time.ZoneId.systemDefault()).year
                                        } catch (_: Exception) {
                                            null
                                        }
                                        if (year != null && year > 1970) {
                                            Text(
                                                text = "An lansare: $year",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { selectCandidateAndFetchGrids(candidate) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2563EB)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Alege", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = Color(0xFF60A5FA),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Se caută coperți 600x900 pe SteamGridDB...",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }
            }

            // Grid Variants Display (2:3 Vertical Posters, ranked by score)
            if (gridVariants.isNotEmpty() && !isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Variante copertă (${gridVariants.size} găsite):",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    if (gameCandidates.size > 1) {
                        Text(
                            text = "Schimbă jocul",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF60A5FA),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    selectedGameCandidate = null
                                    gridVariants = emptyList()
                                }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(gridVariants, key = { it.id }) { grid ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onCoverSelected(grid.url, grid.thumb)
                                    onDismiss()
                                }
                                .testTag("grid_variant_${grid.id}"),
                            color = Color(0xFF1E1B2E),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        .background(Color(0xFF0F172A))
                                ) {
                                    // Use thumbnail in list for high performance
                                    ShelfAsyncImage(
                                        imageUrl = grid.thumb.ifBlank { grid.url },
                                        contentDescription = "SteamGridDB Poster",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Score Badge
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp),
                                        color = Color(0xCC0F172A),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "${grid.score}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                // Selection button under card
                                Button(
                                    onClick = {
                                        onCoverSelected(grid.url, grid.thumb)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2563EB),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                ) {
                                    Text("Alege", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // No Results Found: Authentic Placeholder + Manual alternate search or URL paste
            if (noResultsFound && !isLoading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1B2E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Vertical 2:3 Placeholder Box with Game Title
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                                    )
                                )
                                .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = searchQuery.ifBlank { "Fără copertă" },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Nu am găsit coperți pe SteamGridDB pentru acest titlu.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Option 1: Search manually with another name
                        Text(
                            text = "Opțiunea 1: Caută manual după alt nume",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualAlternateTitle,
                                onValueChange = { manualAlternateTitle = it },
                                placeholder = { Text("Ex: Final Fantasy VII, Doom...", color = Color(0xFF64748B)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_manual_steamgriddb_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF13111C),
                                    unfocusedContainerColor = Color(0xFF13111C)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Button(
                                onClick = {
                                    searchQuery = manualAlternateTitle
                                    searchForGames(manualAlternateTitle)
                                },
                                enabled = manualAlternateTitle.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Caută", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Option 2: Paste custom image URL
                        Text(
                            text = "Opțiunea 2: Lipește un URL de imagine",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualCustomUrl,
                                onValueChange = { manualCustomUrl = it },
                                placeholder = { Text("https://... sau fișier...", color = Color(0xFF64748B)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_manual_image_url"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF13111C),
                                    unfocusedContainerColor = Color(0xFF13111C)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Button(
                                onClick = {
                                    val trimmed = manualCustomUrl.trim()
                                    if (trimmed.isNotBlank()) {
                                        onCoverSelected(trimmed, trimmed)
                                        onDismiss()
                                    }
                                },
                                enabled = manualCustomUrl.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Aplică", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Non-blocking close button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_dismiss_steamgriddb"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Închide (Continuă fără copertă)", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
