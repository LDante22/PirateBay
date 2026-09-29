package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.hltb.HltbLoadingStatus
import com.example.data.metadata.CoverProjectResolver
import com.example.data.metadata.CoverProjectVariant
import com.example.data.model.CoverSourceType
import com.example.data.model.Game
import com.example.data.model.WishlistStatus
import com.example.ui.components.shelf.ShelfAsyncImage

/**
 * Focused Game Detail Screen redesigned to match modern native game-library visual standards:
 * - Large centered game cover / artwork
 * - Game title prominently displayed below the artwork
 * - Console / platform information directly beneath the title
 * - Compact metadata (Release Year, Platform/Genre, Rating, Status)
 * - Short game description with subtle "Read more" interaction
 * - Artwork-first layout without heavy text walls
 * - Compact "TTB" (Time to Beat) section
 * - Emulator: strictly emulator name only (no pipeline, graphics, specs, or bios)
 * - Integrated YouTube trailer player & status controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailSheet(
    game: Game,
    onDismiss: () -> Unit,
    onEdit: (Game) -> Unit,
    onDelete: (Game) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onUpdateStatus: (Long, String) -> Unit,
    onUpdateRating: (Long, Float) -> Unit = { _, _ -> },
    loadingStatus: HltbLoadingStatus = HltbLoadingStatus.IDLE,
    onRefreshHltb: (Game) -> Unit = {},
    isRefreshingMetadata: Boolean = false,
    onRefreshMetadata: (Game) -> Unit = {},
    onOpenVersus: ((Game) -> Unit)? = null,
    onUpdateBackCover: ((Long, String) -> Unit)? = null,
    onApplyCoverVariant: ((Long, CoverProjectVariant) -> Unit)? = null,
    onApplySteamGridDbCover: ((Long, String, String) -> Unit)? = null,
    onRefreshCover: ((Game) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    val console = game.console
    val brandAccent = Color(console.accentColorHex)

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isTrailerExpanded by remember { mutableStateOf(false) }

    var spinCount by remember { mutableIntStateOf(0) }
    var flipCount by remember { mutableIntStateOf(0) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showChangeBackDialog by remember { mutableStateOf(false) }
    var showSteamGridDbModal by remember { mutableStateOf(false) }
    var backCoverUrlInput by remember { mutableStateOf("") }

    val backCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageUriToInternalStorage(context, uri)
            if (savedPath != null && onUpdateBackCover != null) {
                onUpdateBackCover(game.id, savedPath)
            }
        }
    }

    // Fallback description from synopsis or notes
    val description = remember(game) {
        when {
            game.description.isNotBlank() -> game.description
            game.notes.isNotBlank() -> game.notes
            else -> "A classic ${game.genre.ifBlank { "adventure" }} title released for ${console.displayName}."
        }
    }

    // Smooth fade-in & scale entrance animation when opening individual game details
    var isContentVisible by remember(game.id) { mutableStateOf(false) }
    LaunchedEffect(game.id) {
        isContentVisible = true
    }

    val sheetAlpha by animateFloatAsState(
        targetValue = if (isContentVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        label = "detail_fade_in_alpha"
    )

    val sheetScale by animateFloatAsState(
        targetValue = if (isContentVisible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "detail_scale_in"
    )

    val coverScale by animateFloatAsState(
        targetValue = if (isContentVisible) 1f else 0.84f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "detail_cover_scale"
    )

    val coverAlpha by animateFloatAsState(
        targetValue = if (isContentVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 320,
            delayMillis = 40,
            easing = FastOutSlowInEasing
        ),
        label = "detail_cover_alpha"
    )

    val bodyAlpha by animateFloatAsState(
        targetValue = if (isContentVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 360,
            delayMillis = 70,
            easing = FastOutSlowInEasing
        ),
        label = "detail_body_alpha"
    )

    val bodyTranslationY by animateFloatAsState(
        targetValue = if (isContentVisible) 0f else 24f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "detail_body_slide"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131119),
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF484454))
                )
            }
        },
        modifier = modifier.testTag("game_detail_sheet")
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .graphicsLayer {
                    alpha = sheetAlpha
                    scaleX = sheetScale
                    scaleY = sheetScale
                    transformOrigin = TransformOrigin(0.5f, 0.18f)
                }
                .verticalScroll(scrollState)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // TOP ACTIONS BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button on the left
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag("detail_close_btn")
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF221F2C))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Modal",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick actions on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Favorite Toggle
                    IconButton(
                        onClick = { onToggleFavorite(game.id) },
                        modifier = Modifier
                            .testTag("detail_favorite_btn")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (game.isFavorite) Color(0xFF492532) else Color(0xFF221F2C))
                    ) {
                        Icon(
                            imageVector = if (game.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (game.isFavorite) Color(0xFFFF5376) else Color(0xFFCAC4D0),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Versus Picker (if callback provided)
                    if (onOpenVersus != null) {
                        IconButton(
                            onClick = { onOpenVersus(game) },
                            modifier = Modifier
                                .testTag("detail_versus_btn")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF381E72))
                        ) {
                            Text(text = "⚔️", fontSize = 14.sp)
                        }
                    }

                    // Edit
                    IconButton(
                        onClick = { onEdit(game) },
                        modifier = Modifier
                            .testTag("detail_quick_edit_btn")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF221F2C))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Game",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = { onDelete(game) },
                        modifier = Modifier
                            .testTag("detail_delete_btn")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF221F2C))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Game",
                            tint = Color(0xFFFFB4AB),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 1. LARGE CENTERED GAME COVER / ARTWORK (Interactive 3D Physical Card)
            // ==========================================
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                        alpha = coverAlpha
                        transformOrigin = TransformOrigin.Center
                    },
                contentAlignment = Alignment.Center
            ) {
                Interactive3DGameCard(
                    game = game,
                    aspectRatio = 0.70f,
                    hasPhysicalSpine = true,
                    enable3DRotation = true,
                    showDeleteButton = false,
                    showTrailerButton = false,
                    showDetailsButton = false,
                    onImportBackCover = {
                        showChangeBackDialog = true
                    },
                    spinTrigger = spinCount,
                    flipTrigger = flipCount,
                    modifier = Modifier
                        .width(200.dp)
                        .testTag("detail_cover_art")
                )
            }

            // Game details body with smooth upward slide & fade-in choreography
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = bodyAlpha
                        translationY = bodyTranslationY
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Consistent airy vertical padding between cover image and action buttons container
                Spacer(modifier = Modifier.height(28.dp))

                // 3D Physical Case Controls FlowRow (360° Spin, Flip Case, Import Back, Schimbă coperta)
                // Flexibly wraps on small mobile screens without fixed widths or breaking margins
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .testTag("detail_action_buttons_container"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Spin 360° Button
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { spinCount++ }
                            .testTag("btn_spin_360"),
                        color = Color(0xFF221F2D),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate 360°",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "360° Spin",
                                color = Color(0xFFE6E1E5),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 2. Flip Case (Front / Back)
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { flipCount++ }
                            .testTag("btn_flip_case"),
                        color = Color(0xFF221F2D),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "Flip Case",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Flip Case",
                                color = Color(0xFFE6E1E5),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 3. Import / Change Back Cover Button
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                showChangeBackDialog = true
                            }
                            .testTag("btn_import_back"),
                        color = Color(0xFF381E72),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Import Back Image",
                                tint = Color(0xFFEADDFF),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (game.backCoverUrl.isBlank() && !game.hasCompleteCaseArtwork) "Import Back" else "Change Back",
                                color = Color(0xFFEADDFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 4. Schimbă coperta Button (SteamGridDB Official 600x900 Covers)
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                showSteamGridDbModal = true
                            }
                            .testTag("btn_change_cover_sheet"),
                        color = Color(0xFF1E3A8A),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Schimbă coperta",
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Schimbă coperta",
                                color = Color(0xFFDBEAFE),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

            // Cover Source & Fallback Status Badge
            Surface(
                color = Color(0xFF1E1D24),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.6.dp, Color(0xFF3B3846)),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val (sourceLabel, sourceColor) = when (game.coverSource) {
                        CoverSourceType.STEAMGRIDDB -> "Cover Source: SteamGridDB (Official 600x900)" to Color(0xFF60A5FA)
                        CoverSourceType.COVER_PROJECT -> "Cover Source: The Cover Project (Deterministic Fallback)" to Color(0xFF81C784)
                        CoverSourceType.EXISTING -> "Cover Source: Primary (IGDB / MobyGames)" to Color(0xFF90CAF9)
                        CoverSourceType.MANUAL -> "Cover Source: Custom Image" to Color(0xFFFFD54F)
                        CoverSourceType.PLACEHOLDER -> "Cover Source: Local Generated" to Color(0xFFB0BEC5)
                        else -> "Cover Source: Primary" to Color(0xFF90CAF9)
                    }
                    Text(
                        text = sourceLabel,
                        color = sourceColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (onRefreshCover != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onRefreshCover(game) }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Re-check Covers",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Dedicated Change Back Cover Modal Dialog
            // Strictly scoped to CURRENT GAME + CURRENT PLATFORM (Never cross-game)
            if (showChangeBackDialog) {
                val verifiedVariants = remember(game) {
                    CoverProjectResolver.getVerifiedVariantsForGame(game.title, game.console)
                }

                AlertDialog(
                    onDismissRequest = { showChangeBackDialog = false },
                    title = {
                        Column {
                            Text(
                                text = "Back Artwork Selection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${game.title} • ${game.console.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = brandAccent
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                        ) {
                            if (verifiedVariants.isNotEmpty()) {
                                Text(
                                    text = "Verified The Cover Project variants for this game and platform:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFCAC4D0),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    items(verifiedVariants, key = { it.variantId }) { variant ->
                                        Surface(
                                            color = Color(0xFF25232D),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF3F3B4B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 44.dp, height = 60.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color.Black)
                                                ) {
                                                    ShelfAsyncImage(
                                                        imageUrl = variant.backCoverUrl.ifBlank { variant.completeCaseArtworkUrl },
                                                        contentDescription = variant.description.ifBlank { variant.title },
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                        alignment = Alignment.CenterStart
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = variant.description.ifBlank { variant.title },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "${variant.region} • ${if (variant.isCompleteWrap) "Full Physical Insert" else "Back Cover"}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFD0BCFF)
                                                    )
                                                    if (variant.contributor.isNotBlank()) {
                                                        Text(
                                                            text = "By ${variant.contributor}",
                                                            fontSize = 9.sp,
                                                            color = Color.White.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = {
                                                        if (onApplyCoverVariant != null) {
                                                            onApplyCoverVariant(game.id, variant)
                                                        } else if (onUpdateBackCover != null) {
                                                            val targetBack = variant.backCoverUrl.ifBlank { variant.completeCaseArtworkUrl }
                                                            onUpdateBackCover(game.id, targetBack)
                                                        }
                                                        showChangeBackDialog = false
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Apply", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    color = Color(0xFF1E1D24),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.6.dp, Color(0xFF383544)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No Alternative Back Variants",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No alternative back artwork was found in The Cover Project catalog for ${game.title} on ${game.console.displayName}.\n\nTo preserve collection accuracy, artwork from other games or platforms is never substituted.",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFCAC4D0),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Import Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showChangeBackDialog = false
                                        backCoverPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("From Gallery", fontSize = 10.5.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        showChangeBackDialog = false
                                        showUrlDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("Enter URL", fontSize = 10.5.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showChangeBackDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Optional Dialog to input back cover by URL
            if (showUrlDialog) {
                AlertDialog(
                    onDismissRequest = { showUrlDialog = false },
                    title = { Text("Import Back Cover by URL", color = Color.White) },
                    text = {
                        Column {
                            Text(
                                "Enter direct image link for the back cover of ${game.title}:",
                                color = Color(0xFFCAC4D0),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = backCoverUrlInput,
                                onValueChange = { backCoverUrlInput = it },
                                label = { Text("Image URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (backCoverUrlInput.isNotBlank() && onUpdateBackCover != null) {
                                    onUpdateBackCover(game.id, backCoverUrlInput.trim())
                                }
                                showUrlDialog = false
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUrlDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 2. GAME TITLE PROMINENTLY DISPLAYED
            // ==========================================
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 21.sp,
                    color = Color(0xFFF8FAFC),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("detail_game_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 3. CONSOLE / PLATFORM DIRECTLY BENEATH TITLE
            // ==========================================
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(console.brandColorHex).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, brandAccent.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("detail_console_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = console.emoji, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = console.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.4.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // 4. COMPACT METADATA ROW (Year, Genre, Rating)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (game.releaseYear > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF221F2D),
                        border = BorderStroke(1.dp, Color(0xFF332F42))
                    ) {
                        Text(
                            text = "📅 ${game.releaseYear}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (game.genre.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF221F2D),
                        border = BorderStroke(1.dp, Color(0xFF332F42))
                    ) {
                        Text(
                            text = "🏷️ ${game.genre}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFD0BCFF),
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Rating Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2E2417),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", game.userRating),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 5. SHORT GAME DESCRIPTION WITH "READ MORE"
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFC7B8E0),
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (description.length > 120) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isDescriptionExpanded) "Show less" else "Read more",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 6. COMPACT TIME TO BEAT (TTB) SECTION
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1C1926),
                border = BorderStroke(1.dp, Color(0xFF2C273C))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header row: "TTB" with refresh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TTB",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 13.sp,
                                    letterSpacing = 0.8.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• HowLongToBeat",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF7E7892),
                                    fontSize = 10.5.sp
                                )
                            )
                        }

                        val infiniteTransition = rememberInfiniteTransition(label = "detail_hltb_spin")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "spin_angle"
                        )
                        val isLoadingHltb = loadingStatus == HltbLoadingStatus.LOADING

                        IconButton(
                            onClick = { onRefreshHltb(game) },
                            enabled = !isLoadingHltb,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Time to Beat",
                                tint = if (isLoadingHltb) Color(0xFFA5B4FC) else Color(0xFF94A3B8),
                                modifier = Modifier
                                    .size(15.dp)
                                    .then(if (isLoadingHltb) Modifier.rotate(rotation) else Modifier)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Compact 3-metric row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Main Story
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF242031)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Main",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = game.formattedMainStory,
                                    fontSize = 12.sp,
                                    color = Color(0xFF60A5FA),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Main + Extra
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF242031)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Extra",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = game.formattedMainExtra,
                                    fontSize = 12.sp,
                                    color = Color(0xFFA78BFA),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Completionist
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF242031)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "100%",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = game.formattedCompletionist,
                                    fontSize = 12.sp,
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // 7. COMPACT EMULATOR SECTION (Emulator Name Only)
            // ==========================================
            val emulatorName = remember(game) {
                game.emulator.ifBlank { console.primaryEmulator() }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1C1926),
                border = BorderStroke(1.dp, Color(0xFF2C273C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Emulator",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2B2240),
                        border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = emulatorName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0BCFF),
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 8. YOUTUBE TRAILER SECTION
            // ==========================================
            if (game.youtubeVideoId.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1C1926),
                    border = BorderStroke(1.dp, Color(0xFF2C273C))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { isTrailerExpanded = !isTrailerExpanded }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF0000).copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFFFF4E4E),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isTrailerExpanded) "Hide Trailer" else "Watch Official Trailer",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // External YouTube Launcher
                            IconButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/watch?v=${game.youtubeVideoId}")
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open in YouTube",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Expandable Player
                        AnimatedVisibility(visible = isTrailerExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                YouTubeTrailerPlayer(
                                    youtubeVideoId = game.youtubeVideoId,
                                    gameTitle = game.title,
                                    autoPlay = true
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 9. WISHLIST / LIBRARY STATUS SELECTOR
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "STATUS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF8E88A0),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WishlistStatus.entries.forEach { status ->
                        val isCurrentStatus = game.wishlistStatus == status.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onUpdateStatus(game.id, status.id) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isCurrentStatus) Color(status.badgeColorHex).copy(alpha = 0.25f) else Color(0xFF1E1B28),
                            border = BorderStroke(
                                width = if (isCurrentStatus) 1.5.dp else 1.dp,
                                color = if (isCurrentStatus) Color(status.badgeColorHex) else Color(0xFF332F42)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = status.iconEmoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = status.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCurrentStatus) Color.White else Color(0xFFCAC4D0),
                                        fontWeight = if (isCurrentStatus) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 10. INTERACTIVE USER RATING
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "YOUR RATING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF8E88A0),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            val isFilled = game.userRating >= star.toFloat()
                            IconButton(
                                onClick = { onUpdateRating(game.id, star.toFloat()) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Rate $star stars",
                                    tint = if (isFilled) Color(0xFFFBBF24) else Color(0xFF5E5870),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (game.userRating > 0f) "${game.userRating.toInt()} / 5 Stars" else "Not rated",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (game.userRating > 0f) Color(0xFFFDE68A) else Color(0xFF7E7892),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        }
    }

    if (showSteamGridDbModal) {
        SteamGridDbCoverPickerModal(
            initialTitle = game.title,
            onDismiss = { showSteamGridDbModal = false },
            onCoverSelected = { fullUrl, thumbUrl ->
                onApplySteamGridDbCover?.invoke(game.id, fullUrl, thumbUrl)
                showSteamGridDbModal = false
            }
        )
    }
}
}
