package com.example.ui.components.shelf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShelfViewMode

fun getShelfModeIcon(mode: ShelfViewMode): ImageVector {
    return when (mode) {
        ShelfViewMode.CLASSIC_GRID -> Icons.Default.GridView
        ShelfViewMode.STEAM_GRID -> Icons.Default.ViewModule
        ShelfViewMode.RETRO_SHELF -> Icons.Default.TableRows
        ShelfViewMode.BOX_ART_3D -> Icons.Default.ViewInAr
        ShelfViewMode.CAROUSEL -> Icons.Default.ViewCarousel
    }
}

@Composable
fun ShelfViewSelectorBar(
    currentMode: ShelfViewMode,
    onModeSelected: (ShelfViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shelf_view_selector_bar"),
        color = Color(0xFF1E1A29),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF3E394A))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShelfViewMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    val icon = getShelfModeIcon(mode)

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF4F378B) else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "mode_bg_color"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFFFFFFF) else Color(0xFFCAC4D0),
                        label = "mode_content_color"
                    )

                    Surface(
                        modifier = Modifier
                            .testTag("shelf_mode_${mode.id}")
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onModeSelected(mode) },
                        color = bgColor,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) BorderStroke(1.dp, Color(0xFFD0BCFF)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = mode.title,
                                tint = if (isSelected) Color(0xFFD0BCFF) else contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = mode.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    color = contentColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShelfViewSelectorDropdown(
    currentMode: ShelfViewMode,
    onModeSelected: (ShelfViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .testTag("btn_shelf_view_dropdown")
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2B2930))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
        ) {
            Icon(
                imageVector = getShelfModeIcon(currentMode),
                contentDescription = "Digital Shelf Mode: ${currentMode.title}",
                tint = Color(0xFFD0BCFF),
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF242030))
                .border(1.dp, Color(0xFF4F378B), RoundedCornerShape(10.dp))
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "DIGITAL SHELF VIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0BCFF),
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )

            ShelfViewMode.entries.forEach { mode ->
                val isSelected = currentMode == mode
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getShelfModeIcon(mode),
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFFE6E1E5),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF938F99),
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        }
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                    modifier = Modifier
                        .testTag("shelf_option_${mode.id}")
                        .background(if (isSelected) Color(0xFF381E72).copy(alpha = 0.5f) else Color.Transparent)
                )
            }
        }
    }
}
