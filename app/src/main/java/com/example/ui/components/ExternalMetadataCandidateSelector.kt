package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.metadata.ExternalMetadataSearchResult

@Composable
fun ExternalMetadataCandidateSelector(
    candidates: List<ExternalMetadataSearchResult>,
    selectedGameId: String?,
    isLoading: Boolean,
    onSelectCandidate: (ExternalMetadataSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = Color(0xFF1E1A29),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color(0xFFD0BCFF),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Searching external game database (RAWG)...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        return
    }

    if (candidates.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MATCHING RESULTS (${candidates.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0BCFF),
                    letterSpacing = 1.sp
                )
            )
            Text(
                text = "Tap to Auto-Fill Metadata",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFCAC4D0),
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            candidates.take(5).forEachIndexed { index, candidate ->
                val isSelected = candidate.externalGameId == selectedGameId

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .testTag("metadata_candidate_${candidate.externalGameId}")
                        .clickable { onSelectCandidate(candidate) },
                    color = if (isSelected) Color(0xFF38234D) else Color(0xFF231E2E),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF3E394A)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Artwork Thumbnail
                        Box(
                            modifier = Modifier
                                .size(44.dp, 58.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF2C243B), Color(0xFF191424))
                                    )
                                )
                        ) {
                            if (candidate.coverArtUrl.isNotBlank()) {
                                AsyncImage(
                                    model = candidate.coverArtUrl,
                                    contentDescription = "${candidate.title} cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = Color(0xFF8B829C),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        // Info Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Match Badge
                                if (candidate.matchBadge.isNotBlank()) {
                                    Surface(
                                        color = when {
                                            candidate.matchBadge.contains("Exact", ignoreCase = true) -> Color(0xFF14532D)
                                            candidate.matchBadge.contains("Remake", ignoreCase = true) ||
                                                candidate.matchBadge.contains("Remaster", ignoreCase = true) -> Color(0xFF701A75)
                                            else -> Color(0xFF312E81)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = candidate.matchBadge,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                if (candidate.releaseYear > 0) {
                                    Text(
                                        text = "${candidate.releaseYear}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCAC4D0),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                if (candidate.metacriticScore > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${candidate.metacriticScore}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFFBBF24),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = candidate.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFF5EEF8) else Color(0xFFE6E1E5)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = if (candidate.platformsSummary.isNotBlank()) {
                                    "Platforms: ${candidate.platformsSummary}"
                                } else if (candidate.genreDisplay.isNotBlank()) {
                                    candidate.genreDisplay
                                } else {
                                    "External DB Record"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF938F99),
                                    fontSize = 10.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Selected indicator
                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFD0BCFF)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
