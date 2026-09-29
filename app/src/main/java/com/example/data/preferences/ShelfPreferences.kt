package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ShelfViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles SharedPreferences persistence for Digital Shelf layout preferences.
 * Persists the user's selected shelf view mode (Classic Grid, Steam Grid, Retro Shelf, 3D Box Art, Carousel)
 * across application cold starts and process restarts.
 */
class ShelfPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _shelfViewMode = MutableStateFlow(loadShelfViewMode())
    val shelfViewMode: StateFlow<ShelfViewMode> = _shelfViewMode.asStateFlow()

    fun getShelfViewMode(): ShelfViewMode {
        return loadShelfViewMode()
    }

    fun setShelfViewMode(mode: ShelfViewMode) {
        prefs.edit()
            .putString(KEY_SHELF_VIEW_MODE, mode.id)
            .apply()
        _shelfViewMode.value = mode
    }

    private fun loadShelfViewMode(): ShelfViewMode {
        val raw = prefs.getString(KEY_SHELF_VIEW_MODE, ShelfViewMode.CLASSIC_GRID.id)
        return ShelfViewMode.fromId(raw)
    }

    companion object {
        const val PREFS_NAME = "the_tavern_shelf_preferences"
        const val KEY_SHELF_VIEW_MODE = "preferred_shelf_view_mode"
    }
}
