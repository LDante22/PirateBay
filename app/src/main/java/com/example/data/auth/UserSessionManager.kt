package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val email: String = "",
    val displayName: String = "",
    val isLoggedIn: Boolean = false,
    val loginTimestamp: Long = 0L,
    val avatarColorHex: Long = 0xFF6366F1
) {
    val initials: String
        get() {
            if (displayName.isNotBlank()) {
                val parts = displayName.trim().split(" ")
                return if (parts.size > 1) {
                    "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
                } else {
                    displayName.take(2).uppercase()
                }
            }
            if (email.isNotBlank()) {
                val namePart = email.substringBefore("@")
                return namePart.take(2).uppercase()
            }
            return "GT"
        }

    val displayHandle: String
        get() {
            if (displayName.isNotBlank()) return displayName
            if (email.isNotBlank()) return email.substringBefore("@")
            return "Guest"
        }
}

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("the_tavern_user_session", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        val timestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
        val colorHex = prefs.getLong(KEY_AVATAR_COLOR, 0xFF6366F1)

        return UserProfile(
            email = email,
            displayName = displayName,
            isLoggedIn = isLoggedIn,
            loginTimestamp = timestamp,
            avatarColorHex = colorHex
        )
    }

    private val shelfPreferences = com.example.data.preferences.ShelfPreferences(context)

    fun getShelfViewMode(): com.example.data.model.ShelfViewMode {
        return shelfPreferences.getShelfViewMode()
    }

    fun setShelfViewMode(mode: com.example.data.model.ShelfViewMode) {
        shelfPreferences.setShelfViewMode(mode)
    }

    fun login(email: String, displayName: String = ""): UserProfile {
        val cleanEmail = email.trim()
        val derivedName = if (displayName.isNotBlank()) {
            displayName.trim()
        } else {
            cleanEmail.substringBefore("@").replace(".", " ").replace("_", " ").replace("-", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }

        // Generate consistent aesthetic color based on email hash
        val palette = listOf(
            0xFF6366F1, // Indigo
            0xFF8B5CF6, // Violet
            0xFFEC4899, // Pink
            0xFF3B82F6, // Blue
            0xFF10B981, // Emerald
            0xFFF59E0B, // Amber
            0xFF06B6D4  // Cyan
        )
        val color = palette[kotlin.math.abs(cleanEmail.hashCode()) % palette.size]
        val timestamp = System.currentTimeMillis()

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_EMAIL, cleanEmail)
            .putString(KEY_DISPLAY_NAME, derivedName)
            .putLong(KEY_LOGIN_TIMESTAMP, timestamp)
            .putLong(KEY_AVATAR_COLOR, color)
            .apply()

        val newProfile = UserProfile(
            email = cleanEmail,
            displayName = derivedName,
            isLoggedIn = true,
            loginTimestamp = timestamp,
            avatarColorHex = color
        )
        _userProfile.value = newProfile
        return newProfile
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_EMAIL)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_LOGIN_TIMESTAMP)
            .remove(KEY_AVATAR_COLOR)
            .apply()

        _userProfile.value = UserProfile(
            email = "",
            displayName = "",
            isLoggedIn = false,
            loginTimestamp = 0L,
            avatarColorHex = 0xFF6366F1
        )
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_DISPLAY_NAME = "user_display_name"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val KEY_AVATAR_COLOR = "avatar_color"
        private const val KEY_SHELF_VIEW_MODE = "shelf_view_mode"
    }
}
