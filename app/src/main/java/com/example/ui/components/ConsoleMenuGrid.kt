package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameConsole

/**
 * Authentic console card designed in the clean ROMSFUN emulator directory style,
 * adapted with a sleek dark theme, high contrast PlayStation typography, crisp console logo artwork,
 * emulator count badge, and library game counts.
 */
@Composable
fun ConsoleCard(
    console: GameConsole,
    gameCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "card_press_scale"
    )

    val brandAccent = Color(console.accentColorHex)
    val emulatorList = console.defaultEmulators()
    val emulatorCount = emulatorList.size

    Card(
        modifier = modifier
            .testTag("console_card_${console.id}")
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF383542),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F1D26)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ==========================================
            // TOP SECTION: Console Picture / Logo Area
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF14131A),
                                Color(0xFF1A1822)
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background subtle ambient brand glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    brandAccent.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // High-fidelity Logotype Artwork for Console
                ConsoleLogotypeArtwork(
                    console = console,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Clean subtle divider line separating Logo Picture and Information
            HorizontalDivider(
                color = Color(0xFF322F3D),
                thickness = 1.dp
            )

            // ==========================================
            // BOTTOM SECTION: Title & Visible Info Rows
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F1D26))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Console Display Name formatted in official brand typography
                Text(
                    text = when (console) {
                        GameConsole.PSP -> "PSP (2004)"
                        GameConsole.PS_VITA -> "PS Vita (2011)"
                        GameConsole.PS2 -> "PS2 (2000)"
                        GameConsole.PS3 -> "PS3 (2006)"
                        GameConsole.PS4 -> "PS4 (2013)"
                        GameConsole.PS5 -> "PS5 (2020)"
                        GameConsole.GBA -> "Game Boy Advance (2001)"
                        GameConsole.N64 -> "Nintendo 64 (1996)"
                        GameConsole.NINTENDO_DS -> "Nintendo DS (2004)"
                        GameConsole.XBOX -> "Xbox (2001)"
                        GameConsole.XBOX_360 -> "Xbox 360 (2005)"
                        GameConsole.XBOX_ONE -> "Xbox One (2013)"
                        GameConsole.WII -> "Wii (2006)"
                        GameConsole.WII_U -> "Wii U (2012)"
                        GameConsole.GAMECUBE -> "GameCube (2001)"
                        GameConsole.SWITCH -> "Nintendo Switch (2017)"
                        GameConsole.NINTENDO_3DS -> "Nintendo 3DS (2011)"
                        GameConsole.PC -> "PC / Windows (Native)"
                        GameConsole.ALL -> "All Consoles"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        letterSpacing = when (console) {
                            GameConsole.PSP, GameConsole.PS2, GameConsole.PS3, GameConsole.PS4, GameConsole.PS5, GameConsole.PS_VITA -> 0.8.sp
                            else -> 0.2.sp
                        },
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats Row: [ 🕹️ X Emulators ]  and  [ ⬇ Y Games ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Emulator Count with Rose/Pink accent matching RomsFun
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color(0xFFFF5376), // RomsFun Signature Rose Pink
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (emulatorCount > 0) "$emulatorCount Emulators" else "Native",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF85A2)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Right: Game Library count with Downward / Download indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color(0xFFFF5376),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$gameCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6E1E5)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic Logotype rendering matching real-world console brand representations
 * with precision vector drawings and authentic typography.
 */
@Composable
fun ConsoleLogotypeArtwork(
    console: GameConsole,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (console) {
            // ==========================================
            // PLAYSTATION 2 (PS2) - Iconic Linear Monolith Logotype
            // ==========================================
            GameConsole.PS2 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    // Authentic PS2 Dual-Stroke Vector Glyph
                    PS2VectorLogo(modifier = Modifier.size(width = 116.dp, height = 38.dp))
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Official PlayStation®2 logotype font typography
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PlayStation",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFFF1F5F9)
                            )
                        )
                        Text(
                            text = ".2",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFF003791).copy(alpha = 0.9f)
                            )
                        )
                    }
                }
            }

            // ==========================================
            // PLAYSTATION 3 (PS3) - Authentic Chrome/Spider-Man & Slim Logo
            // ==========================================
            GameConsole.PS3 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlayStationOfficialLogoGlyph(modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PS3",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 28.sp,
                                letterSpacing = 1.8.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "™",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "PlayStation®3",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFCAC4D0)
                        )
                    )
                }
            }

            // ==========================================
            // PLAYSTATION PORTABLE (PSP) - Wide Capsule Double-Stroke Logo
            // ==========================================
            GameConsole.PSP -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    // Authentic PSP Vector Dual-Stroke Geometric Logotype
                    PSPVectorLogo(modifier = Modifier.size(width = 128.dp, height = 36.dp))

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "PlayStation®Portable",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.5.sp,
                            color = Color(0xFFCAC4D0)
                        )
                    )
                }
            }

            // ==========================================
            // PLAYSTATION VITA (PS VITA) - Wide Modernist Sans Logo
            // ==========================================
            GameConsole.PS_VITA -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    // Authentic PS VITA Typography: Distinctive P S V I T A
                    PSVitaVectorLogo(modifier = Modifier.size(width = 132.dp, height = 34.dp))

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "PlayStation®Vita",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.7.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // ==========================================
            // PLAYSTATION 4 (PS4)
            // ==========================================
            GameConsole.PS4 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlayStationOfficialLogoGlyph(modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PS4",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 30.sp,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "™",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = Color(0xFF60A5FA)
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PlayStation®4",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF93C5FD)
                        )
                    )
                }
            }

            // ==========================================
            // PLAYSTATION 5 (PS5)
            // ==========================================
            GameConsole.PS5 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlayStationOfficialLogoGlyph(modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PS5",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 30.sp,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "™",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = Color(0xFF38BDF8)
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PlayStation®5",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF7DD3FC)
                        )
                    )
                }
            }

            // ==========================================
            // GAME BOY ADVANCE (GBA)
            // ==========================================
            GameConsole.GBA -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GAME BOY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        )
                    }
                    Surface(
                        color = Color(0xFF7C3AED),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = " ADVANCE ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.5.sp,
                                letterSpacing = 2.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // ==========================================
            // NINTENDO 64 (N64)
            // ==========================================
            GameConsole.N64 -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Isometric N64 3D 'N' Icon
                    Canvas(modifier = Modifier.size(34.dp)) {
                        val s = size.width
                        val stroke = 3.5f

                        // Top Green
                        val topPath = Path().apply {
                            moveTo(s * 0.5f, 0f)
                            lineTo(s * 0.85f, s * 0.22f)
                            lineTo(s * 0.5f, s * 0.44f)
                            lineTo(s * 0.15f, s * 0.22f)
                            close()
                        }
                        drawPath(topPath, color = Color(0xFF22C55E))

                        // Left Blue
                        val leftPath = Path().apply {
                            moveTo(s * 0.15f, s * 0.22f)
                            lineTo(s * 0.5f, s * 0.44f)
                            lineTo(s * 0.5f, s * 0.95f)
                            lineTo(s * 0.15f, s * 0.73f)
                            close()
                        }
                        drawPath(leftPath, color = Color(0xFF3B82F6))

                        // Right Red
                        val rightPath = Path().apply {
                            moveTo(s * 0.5f, s * 0.44f)
                            lineTo(s * 0.85f, s * 0.22f)
                            lineTo(s * 0.85f, s * 0.73f)
                            lineTo(s * 0.5f, s * 0.95f)
                            close()
                        }
                        drawPath(rightPath, color = Color(0xFFEF4444))

                        // Center Yellow Accent
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            center = Offset(s * 0.5f, s * 0.44f),
                            radius = 3.5f
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "NINTENDO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp,
                                color = Color(0xFFFBBF24)
                            )
                        )
                        Text(
                            text = "64",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // ==========================================
            // NINTENDO DS (NDS)
            // ==========================================
            GameConsole.NINTENDO_DS -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Dual Screen Outline Icon
                    Canvas(modifier = Modifier.size(28.dp, 34.dp)) {
                        val w = size.width
                        val h = size.height
                        // Top Screen
                        drawRoundRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(0f, 0f),
                            size = Size(w, h * 0.44f),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(width = 2.5f)
                        )
                        // Bottom Touchscreen
                        drawRoundRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(0f, h * 0.56f),
                            size = Size(w, h * 0.44f),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(width = 2.5f)
                        )
                        // Stylus dot
                        drawCircle(
                            color = Color.White,
                            center = Offset(w * 0.5f, h * 0.78f),
                            radius = 2.5f
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Nintendo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = Color(0xFFCAC4D0)
                            )
                        )
                        Text(
                            text = "DS™",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // ==========================================
            // XBOX (ORIGINAL)
            // ==========================================
            GameConsole.XBOX -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(34.dp)) {
                        val s = size.width
                        // Glowing Green Jewel Sphere
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF4ADE80), Color(0xFF15803D), Color(0xFF052E16)),
                                center = Offset(s * 0.5f, s * 0.5f),
                                radius = s * 0.5f
                            )
                        )
                        // 3D Inset X
                        val stroke = 3.5f
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(s * 0.25f, s * 0.25f),
                            end = Offset(s * 0.75f, s * 0.75f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(s * 0.75f, s * 0.25f),
                            end = Offset(s * 0.25f, s * 0.75f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "XBOX",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "ORIGINAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFF4ADE80)
                            )
                        )
                    }
                }
            }

            // ==========================================
            // XBOX 360
            // ==========================================
            GameConsole.XBOX_360 -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Xbox 360 Ring of Light Orb
                    Canvas(modifier = Modifier.size(32.dp)) {
                        val s = size.width
                        drawCircle(
                            color = Color(0xFF22C55E),
                            center = Offset(s * 0.5f, s * 0.5f),
                            radius = s * 0.45f,
                            style = Stroke(width = 3.5f)
                        )
                        drawCircle(
                            color = Color.White,
                            center = Offset(s * 0.5f, s * 0.5f),
                            radius = s * 0.22f
                        )
                        // Sphere X
                        drawLine(
                            color = Color(0xFF15803D),
                            start = Offset(s * 0.32f, s * 0.32f),
                            end = Offset(s * 0.68f, s * 0.68f),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = Color(0xFF15803D),
                            start = Offset(s * 0.68f, s * 0.32f),
                            end = Offset(s * 0.32f, s * 0.68f),
                            strokeWidth = 2.5f
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "XBOX 360",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 1.2.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "MICROSOFT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFF86EFAC)
                            )
                        )
                    }
                }
            }

            // ==========================================
            // XBOX ONE
            // ==========================================
            GameConsole.XBOX_ONE -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(32.dp)) {
                        val s = size.width
                        drawCircle(
                            color = Color(0xFF10B981),
                            center = Offset(s * 0.5f, s * 0.5f),
                            radius = s * 0.45f
                        )
                        // Cutout X
                        val stroke = 3f
                        drawLine(
                            color = Color(0xFF064E3B),
                            start = Offset(s * 0.28f, s * 0.28f),
                            end = Offset(s * 0.72f, s * 0.72f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFF064E3B),
                            start = Offset(s * 0.72f, s * 0.28f),
                            end = Offset(s * 0.28f, s * 0.72f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "XBOX ONE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.2.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "MICROSOFT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFF6EE7B7)
                            )
                        )
                    }
                }
            }

            // ==========================================
            // NINTENDO SWITCH
            // ==========================================
            GameConsole.SWITCH -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Dual Joy-Con Icon (Neon Cyan & Red)
                    Canvas(modifier = Modifier.size(34.dp, 36.dp)) {
                        val w = size.width
                        val h = size.height
                        // Left Joy-Con (Cyan)
                        drawRoundRect(
                            color = Color(0xFF00C3E3),
                            topLeft = Offset(0f, 0f),
                            size = Size(w * 0.44f, h),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        // Left Joy-con button dot
                        drawCircle(
                            color = Color(0xFF14131A),
                            center = Offset(w * 0.22f, h * 0.32f),
                            radius = 4f
                        )
                        // Left Joy-con inner D-buttons
                        drawCircle(
                            color = Color(0xFF14131A),
                            center = Offset(w * 0.22f, h * 0.68f),
                            radius = 2f
                        )

                        // Right Joy-Con (Neon Red)
                        drawRoundRect(
                            color = Color(0xFFFF3C5A),
                            topLeft = Offset(w * 0.56f, 0f),
                            size = Size(w * 0.44f, h),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        // Right Joy-con ABXY buttons
                        drawCircle(
                            color = Color(0xFF14131A),
                            center = Offset(w * 0.78f, h * 0.32f),
                            radius = 2f
                        )
                        // Right Joy-con button dot
                        drawCircle(
                            color = Color(0xFF14131A),
                            center = Offset(w * 0.78f, h * 0.68f),
                            radius = 4f
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "NINTENDO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 2.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        )
                        Text(
                            text = "SWITCH",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 1.5.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // ==========================================
            // NINTENDO WII
            // ==========================================
            GameConsole.WII -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Wii",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 38.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFFF8FAFC)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "™",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = Color(0xFF0EA5E9)
                            )
                        )
                    }
                    Text(
                        text = "Nintendo",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            // ==========================================
            // NINTENDO WII U
            // ==========================================
            GameConsole.WII_U -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Wii",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFFF8FAFC)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // The iconic vibrant Cyan Wii U "U" badge
                        Surface(
                            color = Color(0xFF00B4D8),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "U",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Nintendo",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90E0EF)
                        )
                    )
                }
            }

            // ==========================================
            // NINTENDO GAMECUBE
            // ==========================================
            GameConsole.GAMECUBE -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Isometric GameCube 'G' Logo
                    Canvas(modifier = Modifier.size(34.dp)) {
                        val s = size.width
                        val stroke = 3.5f

                        // Outer Hexagon
                        val outerPath = Path().apply {
                            moveTo(s * 0.5f, 0f)
                            lineTo(s, s * 0.25f)
                            lineTo(s, s * 0.75f)
                            lineTo(s * 0.5f, s)
                            lineTo(0f, s * 0.75f)
                            lineTo(0f, s * 0.25f)
                            close()
                        }
                        drawPath(
                            path = outerPath,
                            color = Color(0xFFA855F7),
                            style = Stroke(width = stroke)
                        )

                        // Center Isometric Y lines
                        drawLine(
                            color = Color(0xFFA855F7),
                            start = Offset(s * 0.5f, s * 0.5f),
                            end = Offset(s * 0.5f, 0f),
                            strokeWidth = stroke
                        )
                        drawLine(
                            color = Color(0xFFA855F7),
                            start = Offset(s * 0.5f, s * 0.5f),
                            end = Offset(s, s * 0.75f),
                            strokeWidth = stroke
                        )
                        drawLine(
                            color = Color(0xFFA855F7),
                            start = Offset(s * 0.5f, s * 0.5f),
                            end = Offset(0f, s * 0.75f),
                            strokeWidth = stroke
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "NINTENDO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFFD0BCFF)
                            )
                        )
                        Text(
                            text = "GAMECUBE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // ==========================================
            // NINTENDO 3DS
            // ==========================================
            GameConsole.NINTENDO_3DS -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Nintendo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFFCAC4D0)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "3DS",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFFFF3B30)
                        )
                    )
                }
            }

            // ==========================================
            // PC ENGINE / STEAM
            // ==========================================
            GameConsole.PC -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(30.dp)) {
                        val s = size.width
                        // Steam Crank wheel
                        drawCircle(
                            color = Color(0xFF66C0F4),
                            center = Offset(s * 0.5f, s * 0.5f),
                            radius = s * 0.44f,
                            style = Stroke(width = 3.5f)
                        )
                        drawCircle(
                            color = Color(0xFF66C0F4),
                            center = Offset(s * 0.5f, s * 0.5f),
                            radius = s * 0.2f
                        )
                        drawLine(
                            color = Color(0xFF66C0F4),
                            start = Offset(s * 0.5f, s * 0.5f),
                            end = Offset(s * 0.85f, s * 0.85f),
                            strokeWidth = 4f
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "PC ENGINE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "STEAM / WINDOWS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.8.sp,
                                color = Color(0xFF66C0F4)
                            )
                        )
                    }
                }
            }

            // ==========================================
            // ALL CONSOLES VAULT
            // ==========================================
            GameConsole.ALL -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ALL VAULT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.5.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "EVERY CONSOLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFFD0BCFF)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-precision vector renderer for the official PlayStation 2 (PS2) logotype.
 * Accurately replicates the linear geometric segmented P, S, and 2 in Sony's signature style.
 */
@Composable
fun PS2VectorLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 3.2f
        val color = Color.White

        // ---- Letter 'P' ----
        // Vertical stem
        drawLine(
            color = color,
            start = Offset(w * 0.05f, h * 0.08f),
            end = Offset(w * 0.05f, h * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )
        // Top loop of P
        val pPath = Path().apply {
            moveTo(w * 0.05f, h * 0.08f)
            lineTo(w * 0.28f, h * 0.08f)
            lineTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.05f, h * 0.52f)
        }
        drawPath(pPath, color = color, style = Stroke(width = stroke, join = StrokeJoin.Miter))

        // Inner P line
        val pInner = Path().apply {
            moveTo(w * 0.12f, h * 0.22f)
            lineTo(w * 0.21f, h * 0.22f)
            lineTo(w * 0.21f, h * 0.38f)
            lineTo(w * 0.12f, h * 0.38f)
            close()
        }
        drawPath(pInner, color = color, style = Stroke(width = stroke * 0.8f))

        // ---- Letter 'S' ----
        val sPath = Path().apply {
            moveTo(w * 0.62f, h * 0.08f)
            lineTo(w * 0.38f, h * 0.08f)
            lineTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.62f, h * 0.92f)
            lineTo(w * 0.38f, h * 0.92f)
        }
        drawPath(sPath, color = color, style = Stroke(width = stroke, join = StrokeJoin.Miter))

        // ---- Digit '2' ----
        val num2Path = Path().apply {
            moveTo(w * 0.72f, h * 0.08f)
            lineTo(w * 0.95f, h * 0.08f)
            lineTo(w * 0.95f, h * 0.50f)
            lineTo(w * 0.72f, h * 0.50f)
            lineTo(w * 0.72f, h * 0.92f)
            lineTo(w * 0.95f, h * 0.92f)
        }
        drawPath(num2Path, color = color, style = Stroke(width = stroke, join = StrokeJoin.Miter))
    }
}

/**
 * High-precision vector renderer for the official PlayStation Portable (PSP) logotype.
 * Replicates the wide capsule double-contour geometry for P, S, P.
 */
@Composable
fun PSPVectorLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 3.0f
        val color = Color.White

        // --- First P ---
        val p1 = Path().apply {
            moveTo(w * 0.03f, h * 0.92f)
            lineTo(w * 0.03f, h * 0.08f)
            lineTo(w * 0.26f, h * 0.08f)
            cubicTo(w * 0.32f, h * 0.08f, w * 0.32f, h * 0.54f, w * 0.26f, h * 0.54f)
            lineTo(w * 0.03f, h * 0.54f)
        }
        drawPath(p1, color = color, style = Stroke(width = stroke))

        val p1Hole = Path().apply {
            moveTo(w * 0.11f, h * 0.40f)
            lineTo(w * 0.11f, h * 0.22f)
            lineTo(w * 0.21f, h * 0.22f)
            cubicTo(w * 0.24f, h * 0.22f, w * 0.24f, h * 0.40f, w * 0.21f, h * 0.40f)
            close()
        }
        drawPath(p1Hole, color = color, style = Stroke(width = stroke * 0.8f))

        // --- Letter S ---
        val sPath = Path().apply {
            moveTo(w * 0.60f, h * 0.14f)
            cubicTo(w * 0.52f, h * 0.08f, w * 0.40f, h * 0.08f, w * 0.38f, h * 0.26f)
            cubicTo(w * 0.38f, h * 0.46f, w * 0.60f, h * 0.50f, w * 0.60f, h * 0.72f)
            cubicTo(w * 0.60f, h * 0.92f, w * 0.46f, h * 0.92f, w * 0.38f, h * 0.84f)
        }
        drawPath(sPath, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // --- Second P ---
        val p2 = Path().apply {
            moveTo(w * 0.69f, h * 0.92f)
            lineTo(w * 0.69f, h * 0.08f)
            lineTo(w * 0.92f, h * 0.08f)
            cubicTo(w * 0.98f, h * 0.08f, w * 0.98f, h * 0.54f, w * 0.92f, h * 0.54f)
            lineTo(w * 0.69f, h * 0.54f)
        }
        drawPath(p2, color = color, style = Stroke(width = stroke))

        val p2Hole = Path().apply {
            moveTo(w * 0.77f, h * 0.40f)
            lineTo(w * 0.77f, h * 0.22f)
            lineTo(w * 0.87f, h * 0.22f)
            cubicTo(w * 0.90f, h * 0.22f, w * 0.90f, h * 0.40f, w * 0.87f, h * 0.40f)
            close()
        }
        drawPath(p2Hole, color = color, style = Stroke(width = stroke * 0.8f))
    }
}

/**
 * High-precision vector renderer for the official PlayStation Vita (PS VITA) logotype.
 * Replicates the ultra-wide geometric typography with distinctive curved P, S, V, I, T, A.
 */
@Composable
fun PSVitaVectorLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 3.0f
        val color = Color.White

        // --- P ---
        val pPath = Path().apply {
            moveTo(w * 0.04f, h * 0.90f)
            lineTo(w * 0.04f, h * 0.10f)
            lineTo(w * 0.16f, h * 0.10f)
            cubicTo(w * 0.21f, h * 0.10f, w * 0.21f, h * 0.52f, w * 0.16f, h * 0.52f)
            lineTo(w * 0.04f, h * 0.52f)
        }
        drawPath(pPath, color = color, style = Stroke(width = stroke))

        // --- S ---
        val sPath = Path().apply {
            moveTo(w * 0.34f, h * 0.18f)
            cubicTo(w * 0.28f, h * 0.10f, w * 0.22f, h * 0.12f, w * 0.22f, h * 0.28f)
            cubicTo(w * 0.22f, h * 0.48f, w * 0.35f, h * 0.50f, w * 0.35f, h * 0.72f)
            cubicTo(w * 0.35f, h * 0.90f, w * 0.28f, h * 0.90f, w * 0.22f, h * 0.82f)
        }
        drawPath(sPath, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // --- V ---
        val vPath = Path().apply {
            moveTo(w * 0.41f, h * 0.10f)
            lineTo(w * 0.48f, h * 0.90f)
            lineTo(w * 0.55f, h * 0.10f)
        }
        drawPath(vPath, color = color, style = Stroke(width = stroke, join = StrokeJoin.Miter))

        // --- I ---
        drawLine(
            color = color,
            start = Offset(w * 0.61f, h * 0.10f),
            end = Offset(w * 0.61f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )

        // --- T ---
        drawLine(
            color = color,
            start = Offset(w * 0.67f, h * 0.10f),
            end = Offset(w * 0.81f, h * 0.10f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )
        drawLine(
            color = color,
            start = Offset(w * 0.74f, h * 0.10f),
            end = Offset(w * 0.74f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )

        // --- A (Stylized without horizontal crossbar) ---
        val aPath = Path().apply {
            moveTo(w * 0.84f, h * 0.90f)
            lineTo(w * 0.91f, h * 0.10f)
            lineTo(w * 0.98f, h * 0.90f)
        }
        drawPath(aPath, color = color, style = Stroke(width = stroke, join = StrokeJoin.Miter))
    }
}

/**
 * Authentic Sony PlayStation Vector Logo Glyph.
 * Replicates the famous standing P and flat perspective S in official black/white high-contrast.
 */
@Composable
fun PlayStationOfficialLogoGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val color = Color.White

        // Upright P shape
        val pPath = Path().apply {
            moveTo(w * 0.32f, h * 0.08f)
            lineTo(w * 0.52f, h * 0.08f)
            cubicTo(w * 0.74f, h * 0.08f, w * 0.74f, h * 0.50f, w * 0.52f, h * 0.50f)
            lineTo(w * 0.32f, h * 0.50f)
            close()
        }
        drawPath(pPath, color = color)

        // Upright P Stem
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.08f),
            end = Offset(w * 0.34f, h * 0.90f),
            strokeWidth = w * 0.14f,
            cap = StrokeCap.Square
        )

        // Lower perspective S Ribbons
        val sPath1 = Path().apply {
            moveTo(w * 0.12f, h * 0.82f)
            cubicTo(w * 0.38f, h * 0.62f, w * 0.58f, h * 0.82f, w * 0.86f, h * 0.68f)
        }
        drawPath(
            path = sPath1,
            color = color,
            style = Stroke(width = w * 0.11f)
        )

        val sPath2 = Path().apply {
            moveTo(w * 0.22f, h * 0.90f)
            cubicTo(w * 0.44f, h * 0.74f, w * 0.64f, h * 0.90f, w * 0.90f, h * 0.78f)
        }
        drawPath(
            path = sPath2,
            color = color.copy(alpha = 0.85f),
            style = Stroke(width = w * 0.09f)
        )
    }
}

/**
 * Top Header Banner shown when inside a specific console's Game List Menu.
 * Features a Back button to return to the Console Menu, emulator runner badge,
 * and quick platform switch options.
 */
@Composable
fun ConsoleGameListHeader(
    selectedConsole: GameConsole,
    gameCount: Int,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onBackToConsoleMenu: () -> Unit,
    onChangeConsole: (GameConsole) -> Unit,
    onAddGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandColor = Color(selectedConsole.brandColorHex)
    val accentColor = Color(selectedConsole.accentColorHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.8f),
                        Color(0xFF49454F)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = accentColor),
        color = Color(0xFF24222A),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            brandColor.copy(alpha = 0.7f),
                            Color(0xFF1E1D24)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top Row: Back to Consoles Button + Game Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .testTag("btn_back_to_consoles")
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { onBackToConsoleMenu() },
                        color = Color(0xFF381E72),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "← All Consoles",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEADDFF),
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    // Game count indicator
                    Surface(
                        color = Color(0xFF131118).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF49454F))
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "$gameCount matching"
                            } else {
                                "$gameCount ${if (gameCount == 1) "Game" else "Games"}"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (searchQuery.isNotBlank()) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Title & Emulator Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedConsole.emoji,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedConsole.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val emu = selectedConsole.primaryEmulator()
                        if (emu.isNotBlank()) {
                            Text(
                                text = "Target Emulator: $emu  •  ${selectedConsole.mediaType}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        } else {
                            Text(
                                text = selectedConsole.mediaType,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF938F99),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Add Game button for this console
                    Surface(
                        modifier = Modifier
                            .testTag("btn_add_game_for_console")
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAddGame() },
                        color = Color(0xFFD0BCFF),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Game",
                                tint = Color(0xFF381E72),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF381E72),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Console-Specific Search Bar for Quick Title Filtering
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = if (selectedConsole != GameConsole.ALL) {
                                "Search ${selectedConsole.displayName} titles..."
                            } else {
                                "Search all games by title..."
                            },
                            fontSize = 12.5.sp,
                            color = Color(0xFFB0ACB5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Console Collection",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_clear_console_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search query",
                                    tint = Color(0xFFCAC4D0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("console_game_search_bar"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F).copy(alpha = 0.8f),
                        focusedContainerColor = Color(0xFF1B1920).copy(alpha = 0.9f),
                        unfocusedContainerColor = Color(0xFF1B1920).copy(alpha = 0.7f),
                        focusedTextColor = Color(0xFFF5EFF7),
                        unfocusedTextColor = Color(0xFFF5EFF7),
                        cursorColor = Color(0xFFD0BCFF)
                    )
                )
            }
        }
    }
}
