package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.ui.graphics.vector.ImageVector

enum class ShelfViewMode(
    val id: String,
    val title: String,
    val shortLabel: String,
    val description: String
) {
    CLASSIC_GRID(
        id = "classic_grid",
        title = "Classic Grid",
        shortLabel = "Grid",
        description = "Clean modern game store grid with detailed titles & status"
    ),
    STEAM_GRID(
        id = "steam_grid",
        title = "Steam Grid",
        shortLabel = "Steam",
        description = "High-density vertical cover artwork layout"
    ),
    RETRO_SHELF(
        id = "retro_shelf",
        title = "Retro Shelf",
        shortLabel = "Shelf",
        description = "Physical game shelf with wooden planks and standing cases"
    ),
    BOX_ART_3D(
        id = "box_art_3d",
        title = "3D Box Art",
        shortLabel = "3D Boxes",
        description = "3D physical game boxes with visible spine & realistic perspective"
    ),
    CAROUSEL(
        id = "carousel",
        title = "Carousel",
        shortLabel = "Carousel",
        description = "Cinematic horizontal cover flow with hero game details"
    );

    companion object {
        fun fromId(id: String?): ShelfViewMode {
            if (id.isNullOrBlank()) return CLASSIC_GRID
            val clean = id.trim()
            return entries.find { 
                it.id.equals(clean, ignoreCase = true) || 
                it.name.equals(clean, ignoreCase = true) ||
                it.shortLabel.equals(clean, ignoreCase = true) ||
                it.title.equals(clean, ignoreCase = true)
            } ?: CLASSIC_GRID
        }
    }
}
