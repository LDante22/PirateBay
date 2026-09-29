package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.model.CoverSourceType
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.WishlistStatus
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class CloudSyncState(val label: String) {
    IDLE("Ready"),
    SYNCING("Syncing with Firestore..."),
    SYNCED("Synced with Firestore"),
    OFFLINE("Saved to Local Device"),
    ERROR("Sync Notice")
}

data class SyncStatus(
    val state: CloudSyncState = CloudSyncState.IDLE,
    val message: String = "Ready",
    val lastSyncTimestamp: Long = 0L,
    val syncedCount: Int = 0
)

class FirestoreSyncManager(private val context: Context) {

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var activeListener: ListenerRegistration? = null
    private var currentListeningEmail: String? = null
    private var currentSyncJob: Job? = null

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance().apply {
                // Enable offline persistence settings
                firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
                    )
                    .build()
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firebase Firestore unavailable, using local persistence: ${e.message}")
            null
        }
    }

    private fun sanitizeEmailKey(email: String): String {
        return email.trim().lowercase()
            .replace("/", "_")
            .replace(".", "_")
            .replace("@", "_at_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    /**
     * Synchronizes the user's wishlist games with Firestore:
     * 1. Pulls existing cloud games for the user and merges them locally.
     * 2. Pushes local games belonging to this user to Firestore.
     * 3. Sets up a real-time snapshot listener.
     */
    fun startSyncForUser(
        userEmail: String,
        localGames: List<Game>,
        scope: CoroutineScope,
        onMergeRemoteGames: suspend (List<Game>) -> Unit
    ) {
        val cleanEmail = userEmail.trim()
        if (cleanEmail.isBlank()) {
            _syncStatus.value = SyncStatus(
                state = CloudSyncState.OFFLINE,
                message = "Sign in to enable Firestore cloud sync",
                syncedCount = localGames.size
            )
            return
        }

        val db = firestore
        if (db == null) {
            _syncStatus.value = SyncStatus(
                state = CloudSyncState.OFFLINE,
                message = "Saved locally on device",
                lastSyncTimestamp = System.currentTimeMillis(),
                syncedCount = localGames.size
            )
            return
        }

        val userKey = sanitizeEmailKey(cleanEmail)
        val gamesCollection = db.collection("users").document(userKey).collection("games")

        _syncStatus.value = _syncStatus.value.copy(
            state = CloudSyncState.SYNCING,
            message = "Syncing with Firestore..."
        )

        // Cancel any pending sync to prevent duplicate merge race conditions
        currentSyncJob?.cancel()
        currentSyncJob = scope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch remote games
                val snapshot = gamesCollection.get().await()
                val remoteGames = snapshot.documents.mapNotNull { doc ->
                    documentToGame(doc.data, doc.id, cleanEmail)
                }

                // Merge remote games into local database
                if (remoteGames.isNotEmpty()) {
                    onMergeRemoteGames(remoteGames)
                }

                // 2. Upload any user's local games that need backup
                val userLocalGames = localGames.filter {
                    it.userEmail.isBlank() || it.userEmail.equals(cleanEmail, ignoreCase = true)
                }
                for (localGame in userLocalGames) {
                    val docId = if (localGame.id > 0) "game_${localGame.id}" else "game_${System.currentTimeMillis()}"
                    val data = gameToDocument(localGame.copy(userEmail = cleanEmail))
                    gamesCollection.document(docId).set(data, SetOptions.merge()).await()
                }

                val currentCount = if (userLocalGames.size > remoteGames.size) userLocalGames.size else remoteGames.size
                _syncStatus.value = SyncStatus(
                    state = CloudSyncState.SYNCED,
                    message = "Cloud wishlist synchronized",
                    lastSyncTimestamp = System.currentTimeMillis(),
                    syncedCount = currentCount
                )

                // 3. Set up real-time listener if not already listening to this user
                if (currentListeningEmail != cleanEmail) {
                    activeListener?.remove()
                    currentListeningEmail = cleanEmail
                    activeListener = gamesCollection.addSnapshotListener { querySnapshot, error ->
                        if (error != null) {
                            Log.w("FirestoreSyncManager", "Snapshot listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (querySnapshot != null) {
                            val liveGames = querySnapshot.documents.mapNotNull { doc ->
                                documentToGame(doc.data, doc.id, cleanEmail)
                            }
                            scope.launch(Dispatchers.IO) {
                                onMergeRemoteGames(liveGames)
                                _syncStatus.value = SyncStatus(
                                    state = CloudSyncState.SYNCED,
                                    message = "Wishlist up-to-date",
                                    lastSyncTimestamp = System.currentTimeMillis(),
                                    syncedCount = liveGames.size
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Firestore sync exception: ${e.message}", e)
                _syncStatus.value = SyncStatus(
                    state = CloudSyncState.OFFLINE,
                    message = "Stored locally on device (${e.localizedMessage ?: "Offline"})",
                    lastSyncTimestamp = System.currentTimeMillis(),
                    syncedCount = localGames.size
                )
            }
        }
    }

    /**
     * Uploads or updates an individual game to Firestore
     */
    fun saveGameToFirestore(userEmail: String, game: Game, scope: CoroutineScope) {
        if (userEmail.isBlank()) return
        val db = firestore ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val userKey = sanitizeEmailKey(userEmail)
                val docId = if (game.id > 0) "game_${game.id}" else "game_${System.currentTimeMillis()}"
                val data = gameToDocument(game.copy(userEmail = userEmail))
                db.collection("users")
                    .document(userKey)
                    .collection("games")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()

                _syncStatus.value = _syncStatus.value.copy(
                    state = CloudSyncState.SYNCED,
                    message = "Game backed up to Firestore",
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "Failed to save game to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Deletes a game from Firestore
     */
    fun deleteGameFromFirestore(userEmail: String, gameId: Long, scope: CoroutineScope) {
        if (userEmail.isBlank() || gameId <= 0) return
        val db = firestore ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val userKey = sanitizeEmailKey(userEmail)
                val docId = "game_$gameId"
                db.collection("users")
                    .document(userKey)
                    .collection("games")
                    .document(docId)
                    .delete()
                    .await()

                _syncStatus.value = _syncStatus.value.copy(
                    state = CloudSyncState.SYNCED,
                    message = "Game removed from cloud",
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "Failed to delete game from Firestore: ${e.message}")
            }
        }
    }

    fun stopListening() {
        currentSyncJob?.cancel()
        currentSyncJob = null
        activeListener?.remove()
        activeListener = null
        currentListeningEmail = null
        _syncStatus.value = SyncStatus(
            state = CloudSyncState.IDLE,
            message = "Ready",
            lastSyncTimestamp = 0L,
            syncedCount = 0
        )
    }

    private fun gameToDocument(game: Game): Map<String, Any> {
        return hashMapOf(
            "id" to game.id,
            "title" to game.title,
            "consoleId" to game.consoleId,
            "genre" to game.genre,
            "releaseYear" to game.releaseYear,
            "emulator" to game.emulator,
            "youtubeVideoId" to game.youtubeVideoId,
            "coverArtUrl" to game.coverArtUrl,
            "backCoverUrl" to game.backCoverUrl,
            "completeCaseArtwork" to game.completeCaseArtwork,
            "coverSource" to game.coverSource,
            "backCoverSource" to game.backCoverSource,
            "coverGradientStart" to game.coverGradientStart,
            "coverGradientEnd" to game.coverGradientEnd,
            "coverAccentColor" to game.coverAccentColor,
            "wishlistStatus" to game.wishlistStatus,
            "userRating" to game.userRating.toDouble(),
            "targetPrice" to game.targetPrice,
            "developer" to game.developer,
            "notes" to game.notes,
            "isFavorite" to game.isFavorite,
            "addedTimestamp" to game.addedTimestamp,
            "userEmail" to game.userEmail,
            "emulatorNotes" to game.emulatorNotes,
            "recommendedBackend" to game.recommendedBackend,
            "internalResolution" to game.internalResolution,
            "compatibilityRating" to game.compatibilityRating,
            "biosRequirement" to game.biosRequirement,
            "targetFramerate" to game.targetFramerate,
            "controllerLayout" to game.controllerLayout,
            "proTips" to game.proTips,
            "hltbId" to game.hltbId,
            "hltbName" to game.hltbName,
            "hltbMainStoryHours" to game.hltbMainStoryHours.toDouble(),
            "hltbMainExtraHours" to game.hltbMainExtraHours.toDouble(),
            "hltbCompletionistHours" to game.hltbCompletionistHours.toDouble(),
            "hltbLastUpdated" to game.hltbLastUpdated,
            "hltbSyncStatus" to game.hltbSyncStatus,
            "externalProvider" to game.externalProvider,
            "externalGameId" to game.externalGameId,
            "externalLastUpdated" to game.externalLastUpdated,
            "externalSyncStatus" to game.externalSyncStatus,
            "bannerArtUrl" to game.bannerArtUrl,
            "publisher" to game.publisher,
            "releaseDate" to game.releaseDate,
            "description" to game.description,
            "websiteUrl" to game.websiteUrl,
            "metacriticScore" to game.metacriticScore,
            "supportedPlatforms" to game.supportedPlatforms
        )
    }

    private fun documentToGame(data: Map<String, Any>?, docId: String, userEmail: String): Game? {
        if (data == null) return null
        val title = data["title"] as? String ?: return null
        val consoleId = data["consoleId"] as? String ?: GameConsole.PS2.id

        val rawId = (data["id"] as? Number)?.toLong() ?: run {
            docId.removePrefix("game_").toLongOrNull() ?: 0L
        }

        return Game(
            id = rawId,
            title = title,
            consoleId = consoleId,
            genre = data["genre"] as? String ?: "Action",
            releaseYear = (data["releaseYear"] as? Number)?.toInt() ?: 2010,
            emulator = data["emulator"] as? String ?: "",
            youtubeVideoId = data["youtubeVideoId"] as? String ?: "",
            coverArtUrl = data["coverArtUrl"] as? String ?: "",
            backCoverUrl = data["backCoverUrl"] as? String ?: "",
            completeCaseArtwork = data["completeCaseArtwork"] as? String ?: "",
            coverSource = data["coverSource"] as? String ?: CoverSourceType.EXISTING,
            backCoverSource = data["backCoverSource"] as? String ?: "",
            coverGradientStart = (data["coverGradientStart"] as? Number)?.toLong() ?: 0xFF1E1B4BL,
            coverGradientEnd = (data["coverGradientEnd"] as? Number)?.toLong() ?: 0xFF0F172AL,
            coverAccentColor = (data["coverAccentColor"] as? Number)?.toLong() ?: 0xFF6366F1L,
            wishlistStatus = data["wishlistStatus"] as? String ?: WishlistStatus.WANT_TO_PLAY.id,
            userRating = (data["userRating"] as? Number)?.toFloat() ?: 4.5f,
            targetPrice = data["targetPrice"] as? String ?: "$29.99",
            developer = data["developer"] as? String ?: "",
            notes = data["notes"] as? String ?: "",
            isFavorite = data["isFavorite"] as? Boolean ?: false,
            addedTimestamp = (data["addedTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            userEmail = (data["userEmail"] as? String)?.takeIf { it.isNotBlank() } ?: userEmail,
            emulatorNotes = data["emulatorNotes"] as? String ?: "",
            recommendedBackend = data["recommendedBackend"] as? String ?: "",
            internalResolution = data["internalResolution"] as? String ?: "",
            compatibilityRating = data["compatibilityRating"] as? String ?: "",
            biosRequirement = data["biosRequirement"] as? String ?: "",
            targetFramerate = data["targetFramerate"] as? String ?: "",
            controllerLayout = data["controllerLayout"] as? String ?: "",
            proTips = data["proTips"] as? String ?: "",
            hltbId = (data["hltbId"] as? Number)?.toLong() ?: 0L,
            hltbName = data["hltbName"] as? String ?: "",
            hltbMainStoryHours = (data["hltbMainStoryHours"] as? Number)?.toFloat() ?: 0f,
            hltbMainExtraHours = (data["hltbMainExtraHours"] as? Number)?.toFloat() ?: 0f,
            hltbCompletionistHours = (data["hltbCompletionistHours"] as? Number)?.toFloat() ?: 0f,
            hltbLastUpdated = (data["hltbLastUpdated"] as? Number)?.toLong() ?: 0L,
            hltbSyncStatus = data["hltbSyncStatus"] as? String ?: "",
            externalProvider = data["externalProvider"] as? String ?: "RAWG",
            externalGameId = data["externalGameId"] as? String ?: "",
            externalLastUpdated = (data["externalLastUpdated"] as? Number)?.toLong() ?: 0L,
            externalSyncStatus = data["externalSyncStatus"] as? String ?: "",
            bannerArtUrl = data["bannerArtUrl"] as? String ?: "",
            publisher = data["publisher"] as? String ?: "",
            releaseDate = data["releaseDate"] as? String ?: "",
            description = data["description"] as? String ?: "",
            websiteUrl = data["websiteUrl"] as? String ?: "",
            metacriticScore = (data["metacriticScore"] as? Number)?.toInt() ?: 0,
            supportedPlatforms = data["supportedPlatforms"] as? String ?: ""
        )
    }
}
