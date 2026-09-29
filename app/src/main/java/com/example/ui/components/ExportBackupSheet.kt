package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.UserProfile
import com.example.data.backup.BackupOperationState
import com.example.data.backup.ConflictResolutionStrategy
import com.example.data.backup.ExportFormat
import com.example.data.backup.ImportAnalysisResult
import com.example.data.backup.LibraryBackupService
import com.example.data.model.Game
import com.example.data.sync.CloudSyncState
import com.example.data.sync.SyncStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBackupSheet(
    games: List<Game>,
    userProfile: UserProfile,
    syncStatus: SyncStatus,
    backupState: BackupOperationState,
    onSyncNow: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    onAnalyzeFileUri: (Context, Uri) -> Unit,
    onAnalyzeJsonText: (String) -> Unit,
    onConfirmRestore: (ImportAnalysisResult, ConflictResolutionStrategy) -> Unit,
    onCancelImportPreview: () -> Unit,
    onResetBackupState: () -> Unit,
    onSaveToUri: (Context, Uri, String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Export, 1: Import & Restore, 2: Cloud Sync

    // File Creation (Save to storage) launcher
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingExportFormat by remember { mutableStateOf(ExportFormat.JSON) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(pendingExportFormat.mimeType)
    ) { uri: Uri? ->
        if (uri != null && pendingExportContent != null) {
            onSaveToUri(context, uri, pendingExportContent!!) { success ->
                if (success) {
                    Toast.makeText(context, "Export saved successfully to device!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save export file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // File Picker (Import from storage) launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onAnalyzeFileUri(context, uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1B24),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        modifier = Modifier.testTag("export_backup_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(bottom = 16.dp)
        ) {
            // Sheet Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF381E72).copy(alpha = 0.7f), Color(0xFF1E1B24))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD0BCFF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Library Backup & Restore",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC),
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "${games.size} games in your personal vault",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B2930))
                            .testTag("close_backup_sheet_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFCAC4D0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF2B2930),
                contentColor = Color(0xFFD0BCFF),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFFD0BCFF),
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Export", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("backup_tab_export")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Restore", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("backup_tab_restore")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Cloud Sync", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("backup_tab_cloud")
                )
            }

            // Operation Progress or Status Notice
            when (backupState) {
                is BackupOperationState.Exporting -> {
                    Surface(
                        color = Color(0xFF381E72).copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFD0BCFF)
                                )
                                Text(
                                    text = backupState.message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFF8FAFC),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            if (backupState.total > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { backupState.current.toFloat() / backupState.total.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFFD0BCFF),
                                    trackColor = Color(0xFF49454F)
                                )
                            }
                        }
                    }
                }
                is BackupOperationState.ImportAnalyzing -> {
                    Surface(
                        color = Color(0xFF381E72).copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFD0BCFF)
                            )
                            Text(
                                text = backupState.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFF8FAFC),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
                is BackupOperationState.Importing -> {
                    Surface(
                        color = Color(0xFF381E72).copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFD0BCFF)
                                )
                                Text(
                                    text = backupState.message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFF8FAFC),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            if (backupState.total > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { backupState.current.toFloat() / backupState.total.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFFD0BCFF),
                                    trackColor = Color(0xFF49454F)
                                )
                            }
                        }
                    }
                }
                is BackupOperationState.Completed -> {
                    Surface(
                        color = Color(0xFF1B4D3E),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = backupState.message,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (backupState.details.isNotBlank()) {
                                        Text(
                                            text = backupState.details,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF6EE7B7),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = onResetBackupState,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                is BackupOperationState.Error -> {
                    Surface(
                        color = Color(0xFF601410),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB4AB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = backupState.message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFFFB4AB),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            IconButton(
                                onClick = onResetBackupState,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFFFFB4AB),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                else -> {}
            }

            // Tab Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // ==========================================
                        // TAB 0: EXPORT OPTIONS
                        // ==========================================
                        item {
                            // Privacy & Security Guarantee Banner
                            SecurityGuaranteeCard()
                        }

                        item {
                            // JSON Export Option Card
                            ExportOptionCard(
                                title = "Export Full Library (JSON)",
                                extensionLabel = ".JSON",
                                description = "Complete structured backup containing all games, platforms, cover art, release details, HLTB playtimes, and emulator configurations. Can be restored anytime.",
                                icon = Icons.Default.Code,
                                iconBgColor = Color(0xFF381E72),
                                iconTint = Color(0xFFD0BCFF),
                                badges = listOf("${games.size} Games", "Full Metadata", "Offline Ready"),
                                onSaveFile = {
                                    if (games.isEmpty()) {
                                        Toast.makeText(context, "Library is empty.", Toast.LENGTH_SHORT).show()
                                        return@ExportOptionCard
                                    }
                                    pendingExportFormat = ExportFormat.JSON
                                    coroutineScope.launch {
                                        val content = LibraryBackupService.generateJsonExport(games)
                                        pendingExportContent = content
                                        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        createDocumentLauncher.launch("the_tavern_library_backup_$timestampStr.json")
                                    }
                                },
                                onShare = {
                                    if (games.isEmpty()) {
                                        Toast.makeText(context, "Library is empty.", Toast.LENGTH_SHORT).show()
                                        return@ExportOptionCard
                                    }
                                    coroutineScope.launch {
                                        val content = LibraryBackupService.generateJsonExport(games)
                                        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        val fileName = "the_tavern_library_backup_$timestampStr.json"
                                        val shareIntent = LibraryBackupService.createShareIntent(
                                            context,
                                            ExportFormat.JSON,
                                            content,
                                            fileName
                                        )
                                        context.startActivity(Intent.createChooser(shareIntent, "Share JSON Library Backup"))
                                    }
                                },
                                onCopy = {
                                    if (games.isEmpty()) {
                                        Toast.makeText(context, "Library is empty.", Toast.LENGTH_SHORT).show()
                                        return@ExportOptionCard
                                    }
                                    coroutineScope.launch {
                                        val content = LibraryBackupService.generateJsonExport(games)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Game Library Backup", content)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "JSON backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                testTag = "export_json_card"
                            )
                        }

                        item {
                            // CSV Export Option Card
                            ExportOptionCard(
                                title = "Export Spreadsheet (CSV)",
                                extensionLabel = ".CSV",
                                description = "Spreadsheet-compatible export with columns for title, platform, status, rating, HLTB hours, developer, and release year. Opens cleanly in Excel, Google Sheets, and Numbers.",
                                icon = Icons.Default.TableChart,
                                iconBgColor = Color(0xFF1E3A2F),
                                iconTint = Color(0xFF6EE7B7),
                                badges = listOf("UTF-8 BOM", "Excel & Sheets", "RFC 4180"),
                                onSaveFile = {
                                    if (games.isEmpty()) {
                                        Toast.makeText(context, "Library is empty.", Toast.LENGTH_SHORT).show()
                                        return@ExportOptionCard
                                    }
                                    pendingExportFormat = ExportFormat.CSV
                                    coroutineScope.launch {
                                        val content = LibraryBackupService.generateCsvExport(games)
                                        pendingExportContent = content
                                        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        createDocumentLauncher.launch("the_tavern_games_$timestampStr.csv")
                                    }
                                },
                                onShare = {
                                    if (games.isEmpty()) {
                                        Toast.makeText(context, "Library is empty.", Toast.LENGTH_SHORT).show()
                                        return@ExportOptionCard
                                    }
                                    coroutineScope.launch {
                                        val content = LibraryBackupService.generateCsvExport(games)
                                        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        val fileName = "the_tavern_games_$timestampStr.csv"
                                        val shareIntent = LibraryBackupService.createShareIntent(
                                            context,
                                            ExportFormat.CSV,
                                            content,
                                            fileName
                                        )
                                        context.startActivity(Intent.createChooser(shareIntent, "Share CSV Spreadsheet"))
                                    }
                                },
                                onCopy = {
                                    if (games.isEmpty()) {
                                        Toast.makeText(context, "Library is empty.", Toast.LENGTH_SHORT).show()
                                        return@ExportOptionCard
                                    }
                                    coroutineScope.launch {
                                        val content = LibraryBackupService.generateCsvExport(games)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Game Library CSV", content)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                testTag = "export_csv_card"
                            )
                        }
                    }

                    1 -> {
                        // ==========================================
                        // TAB 1: IMPORT & RESTORE OPTIONS
                        // ==========================================
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("restore_info_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                                border = BorderStroke(1.dp, Color(0xFF49454F))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileUpload,
                                            contentDescription = null,
                                            tint = Color(0xFFD0BCFF),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Restore from JSON Backup",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF8FAFC),
                                                fontSize = 15.sp
                                            )
                                        )
                                    }

                                    Text(
                                        text = "Select a valid .json backup file created previously. You will be able to preview all games, detect duplicates, and choose whether to merge or replace before anything is applied.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFCAC4D0),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            openDocumentLauncher.launch("application/json")
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("select_backup_file_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD0BCFF),
                                            contentColor = Color(0xFF381E72)
                                        )
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Select Backup File (.json)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Direct Paste JSON Backup Card
                            var isPasteOpen by remember { mutableStateOf(false) }
                            var pastedText by remember { mutableStateOf("") }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("paste_json_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                                border = BorderStroke(1.dp, Color(0xFF49454F))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isPasteOpen = !isPasteOpen },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = Color(0xFFD0BCFF),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Paste Backup JSON Directly",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF8FAFC),
                                                    fontSize = 14.sp
                                                )
                                            )
                                        }
                                        Text(
                                            text = if (isPasteOpen) "Hide" else "Show",
                                            color = Color(0xFFD0BCFF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    AnimatedVisibility(visible = isPasteOpen) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = pastedText,
                                                onValueChange = { pastedText = it },
                                                placeholder = { Text("Paste {\"format\": \"...\", \"games\": [...]} here...") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .testTag("pasted_json_input"),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFFD0BCFF),
                                                    unfocusedBorderColor = Color(0xFF49454F),
                                                    focusedContainerColor = Color(0xFF1E1B24),
                                                    unfocusedContainerColor = Color(0xFF1E1B24),
                                                    focusedTextColor = Color(0xFFF8FAFC),
                                                    unfocusedTextColor = Color(0xFFF8FAFC)
                                                )
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                                        if (text.isNotBlank()) {
                                                            pastedText = text
                                                        } else {
                                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.6f)),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF))
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Paste Clipboard", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        if (pastedText.isNotBlank()) {
                                                            onAnalyzeJsonText(pastedText)
                                                        }
                                                    },
                                                    enabled = pastedText.isNotBlank(),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("analyze_pasted_json_btn"),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFD0BCFF),
                                                        contentColor = Color(0xFF381E72)
                                                    )
                                                ) {
                                                    Text("Analyze JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // ==========================================
                        // TAB 2: CLOUD SYNCHRONIZATION (FIRESTORE)
                        // ==========================================
                        item {
                            CloudSyncManagementCard(
                                userProfile = userProfile,
                                syncStatus = syncStatus,
                                gamesCount = games.size,
                                onSyncNow = onSyncNow
                            )
                        }

                        item {
                            ReinstallRecoveryGuideCard()
                        }
                    }
                }
            }
        }
    }

    // Modal Import Preview Dialog when a backup file is analyzed
    if (backupState is BackupOperationState.ImportPreview) {
        ImportPreviewDialog(
            analysis = backupState.analysis,
            onConfirm = { strategy ->
                onConfirmRestore(backupState.analysis, strategy)
            },
            onDismiss = onCancelImportPreview
        )
    }
}

@Composable
private fun SecurityGuaranteeCard() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E3A2F).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color(0xFF6EE7B7).copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_guarantee_card")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF6EE7B7),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Zero Credentials or Secrets Exported",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF6EE7B7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = "Exports contain only your game data, metadata, and custom notes. No passwords, tokens, API keys, or private auth IDs are ever written to export files.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    extensionLabel: String,
    description: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    badges: List<String>,
    onSaveFile: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        border = BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = iconBgColor,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC),
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = extensionLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = iconTint,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )

            // Feature Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                badges.forEach { badge ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E1B24),
                        border = BorderStroke(0.5.dp, Color(0xFF49454F))
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFD0BCFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF49454F).copy(alpha = 0.5f), thickness = 0.5.dp)

            // Action Buttons: Save to Files / Share / Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSaveFile,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp)
                        .testTag("${testTag}_save_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF381E72),
                        contentColor = Color(0xFFD0BCFF)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("${testTag}_share_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF49454F)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF8FAFC))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1B24))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                        .testTag("${testTag}_copy_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudSyncManagementCard(
    userProfile: UserProfile,
    syncStatus: SyncStatus,
    gamesCount: Int,
    onSyncNow: () -> Unit
) {
    FirestoreSyncStatusCard(
        userProfile = userProfile,
        syncStatus = syncStatus,
        onSyncNow = onSyncNow
    )
}

@Composable
private fun ReinstallRecoveryGuideCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recovery_guide_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        border = BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Restoring After OS / App Reinstall",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        fontSize = 14.sp
                    )
                )
            }

            Text(
                text = "• Cloud Restore: If you were signed into an email account, simply sign into the same email after reinstalling to automatically restore your entire cloud library.\n\n• Local File Restore: If you created a JSON backup file (.json), navigate to the \"Restore\" tab anytime, select your backup file, and choose your preferred merge strategy to instantly restore your library.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun ImportPreviewDialog(
    analysis: ImportAnalysisResult,
    onConfirm: (ConflictResolutionStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(ConflictResolutionStrategy.MERGE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .testTag("import_preview_dialog"),
        containerColor = Color(0xFF1E1B24),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Backup Restore Preview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Badges Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "In Backup",
                        value = "${analysis.totalRecordsInBackup}",
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "New Games",
                        value = "${analysis.newGames.size}",
                        color = Color(0xFF6EE7B7),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Matches",
                        value = "${analysis.existingMatches.size}",
                        color = Color(0xFFFDE047),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (analysis.invalidRecordsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF601410),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ ${analysis.invalidRecordsCount} invalid or unparseable record(s) will be skipped.",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFFB4AB),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Conflict Resolution Strategy Selection
                if (analysis.hasConflicts) {
                    Text(
                        text = "Choose Conflict Resolution Strategy:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConflictResolutionStrategy.entries.forEach { strategy ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedStrategy == strategy) Color(0xFF381E72).copy(alpha = 0.6f) else Color(0xFF2B2930),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedStrategy == strategy) Color(0xFFD0BCFF) else Color(0xFF49454F)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStrategy = strategy }
                                    .testTag("strategy_${strategy.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedStrategy == strategy,
                                        onClick = { selectedStrategy = strategy },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFFD0BCFF),
                                            unselectedColor = Color(0xFF938F99)
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${strategy.iconEmoji} ${strategy.title}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFF8FAFC),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Text(
                                            text = strategy.description,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFCAC4D0),
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E3A2F),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                            Text(
                                text = "All ${analysis.newGames.size} games are brand new and will be safely added without any conflicts.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStrategy) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_restore_btn")
            ) {
                Text(
                    text = "Restore ${analysis.totalValidGames} Games",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_restore_preview_btn")
            ) {
                Text("Cancel", color = Color(0xFFCAC4D0), fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF2B2930),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 16.sp
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF938F99),
                    fontSize = 10.sp
                )
            )
        }
    }
}
