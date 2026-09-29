package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.auth.UserProfile
import com.example.data.sync.CloudSyncState
import com.example.data.sync.SyncStatus
import com.example.ui.viewmodel.LibraryStats

@Composable
fun UserAccountDialog(
    userProfile: UserProfile,
    libraryStats: LibraryStats,
    syncStatus: SyncStatus = SyncStatus(),
    onSyncNow: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onLogin: (email: String, name: String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var emailInput by remember { mutableStateOf(userProfile.email) }
    var nameInput by remember { mutableStateOf(userProfile.displayName) }
    var isSwitchingAccount by remember { mutableStateOf(!userProfile.isLoggedIn) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("user_account_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B24)),
            border = BorderStroke(1.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF381E72),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = if (userProfile.isLoggedIn && !isSwitchingAccount) "User Account" else "Sign In with Email",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B2930))
                            .testTag("close_account_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFE6E1E5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (userProfile.isLoggedIn && !isSwitchingAccount) {
                    // Logged In Profile View
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Surface(
                            shape = CircleShape,
                            color = Color(userProfile.avatarColorHex),
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userProfile.initials,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = userProfile.displayHandle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = userProfile.email,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFD0BCFF),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Summary Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF2B2930),
                            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "LIBRARY SYNC & STORAGE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFCAC4D0),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    StatItem("Saved Games", "${libraryStats.totalCount}", Color(0xFFD0BCFF))
                                    StatItem("Want to Play", "${libraryStats.wantToPlayCount}", Color(0xFF818CF8))
                                    StatItem("In Collection", "${libraryStats.ownedCount}", Color(0xFF38BDF8))
                                    StatItem("Playing", "${libraryStats.playingCount}", Color(0xFF34D399))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Firebase Firestore Cloud Sync & Last Sync Status Indicator
                        FirestoreSyncStatusCard(
                            userProfile = userProfile,
                            syncStatus = syncStatus,
                            onSyncNow = onSyncNow
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Export & Backup Library Button Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2B2930),
                            border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenBackup()
                                }
                                .testTag("account_dialog_open_backup_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF381E72),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Storage,
                                                contentDescription = null,
                                                tint = Color(0xFFD0BCFF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Export & Backup Library",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF8FAFC),
                                                fontSize = 13.sp
                                            )
                                        )
                                        Text(
                                            text = "JSON Backup • CSV • Restore",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFD0BCFF),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF381E72),
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Text(
                                        text = "Open",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Switch Account / Sign Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isSwitchingAccount = true
                                    emailInput = userProfile.email
                                    nameInput = userProfile.displayName
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF))
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Email", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onLogout()
                                    isSwitchingAccount = true
                                    emailInput = ""
                                    nameInput = ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB3261E).copy(alpha = 0.85f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Sign In Form View
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enter your email to sign in. Any game you add will stay safely saved in your library.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCAC4D0),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email Field
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                errorMessage = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("name@example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFD0BCFF))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedContainerColor = Color(0xFF2B2930),
                                unfocusedContainerColor = Color(0xFF2B2930),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Name / Handle (Optional)
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Display Name / Gamertag (Optional)") },
                            placeholder = { Text("e.g. MasterGamer") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFCAC4D0))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedContainerColor = Color(0xFF2B2930),
                                unfocusedContainerColor = Color(0xFF2B2930),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC)
                            )
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFF87171),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick suggestion pills for quick login
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                label = "ldante2211@gmail.com",
                                onClick = {
                                    emailInput = "ldante2211@gmail.com"
                                    if (nameInput.isBlank()) nameInput = "Dante"
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val trimmedEmail = emailInput.trim()
                                if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                                    errorMessage = "Please enter a valid email address (e.g. name@domain.com)"
                                    return@Button
                                }
                                onLogin(trimmedEmail, nameInput.trim())
                                isSwitchingAccount = false
                                successNotice = "Signed in as $trimmedEmail"
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("sign_in_submit_btn"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            )
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign In & Save Games",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (userProfile.isLoggedIn) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { isSwitchingAccount = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF49454F))
                            ) {
                                Text("Cancel", color = Color(0xFFCAC4D0))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Offline Export & Backup link
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2B2930),
                            border = BorderStroke(1.dp, Color(0xFF49454F)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenBackup()
                                }
                                .testTag("offline_open_backup_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Export & Backup Library (Offline)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFD0BCFF),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
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

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = Color(0xFF938F99)
            )
        )
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF36343B),
        border = BorderStroke(1.dp, Color(0xFF49454F)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(12.dp))
            Text(
                text = label,
                color = Color(0xFFE6E1E5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
