package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.*
import kotlin.math.min

@Composable
fun LudoBoard(
    tokens: List<LudoToken>,
    activePlayer: LudoColor,
    validSelectableTokens: List<LudoToken>,
    onTokenClick: (LudoToken) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine cell coordinate allocations
    val tokensByCoordinate = remember(tokens) {
        tokens.groupBy { LudoBoardHelper.getCoordinate(it) }
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1EFE9))
            .border(2.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
    ) {
        val boardSize = min(constraints.maxWidth, constraints.maxHeight)
        val density = LocalDeviceDensity
        val cellSize = boardSize / 15f

        // 1. Draw static grid and color fields
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / 15f
            val cellH = size.height / 15f

            // --- Home Yards ---
            val yardBrushRed = Brush.linearGradient(listOf(Color(0xFFFF8A80), Color(0xFFEF5350)))
            val yardBrushGreen = Brush.linearGradient(listOf(Color(0xFFB9F6CA), Color(0xFF66BB6A)))
            val yardBrushYellow = Brush.linearGradient(listOf(Color(0xFFFFFF8D), Color(0xFFFFEE58)))
            val yardBrushBlue = Brush.linearGradient(listOf(Color(0xFF82B1FF), Color(0xFF42A5F5)))

            // Red Yard (Bottom Left: rows 9..14, cols 0..5)
            drawRect(brush = yardBrushRed, topLeft = Offset(0f, cellH * 9), size = androidx.compose.ui.geometry.Size(cellW * 6, cellH * 6))
            // Green Yard (Top Left: rows 0..5, cols 0..5)
            drawRect(brush = yardBrushGreen, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cellW * 6, cellH * 6))
            // Yellow Yard (Top Right: rows 0..5, cols 9..14)
            drawRect(brush = yardBrushYellow, topLeft = Offset(cellW * 9, 0f), size = androidx.compose.ui.geometry.Size(cellW * 6, cellH * 6))
            // Blue Yard (Bottom Right: rows 9..14, cols 9..14)
            drawRect(brush = yardBrushBlue, topLeft = Offset(cellW * 9, cellH * 9), size = androidx.compose.ui.geometry.Size(cellW * 6, cellH * 6))

            // White center inner boxes for Yards
            drawRoundRect(Color.White, topLeft = Offset(cellW, cellH * 10), size = androidx.compose.ui.geometry.Size(cellW * 4, cellH * 4), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f))
            drawRoundRect(Color.White, topLeft = Offset(cellW, cellH), size = androidx.compose.ui.geometry.Size(cellW * 4, cellH * 4), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f))
            drawRoundRect(Color.White, topLeft = Offset(cellW * 10, cellH), size = androidx.compose.ui.geometry.Size(cellW * 4, cellH * 4), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f))
            drawRoundRect(Color.White, topLeft = Offset(cellW * 10, cellH * 10), size = androidx.compose.ui.geometry.Size(cellW * 4, cellH * 4), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f))

            // Draw Yard Circular Token Wells
            val wellRadius = min(cellW, cellH) * 0.45f
            val redWells = listOf(Offset(cellW * 2f, cellH * 11f), Offset(cellW * 2f, cellH * 13f), Offset(cellW * 4f, cellH * 11f), Offset(cellW * 4f, cellH * 13f))
            redWells.forEach { drawCircle(Color(0xFFEF5350), radius = wellRadius, center = it) }

            val greenWells = listOf(Offset(cellW * 2f, cellH * 2f), Offset(cellW * 2f, cellH * 4f), Offset(cellW * 4f, cellH * 2f), Offset(cellW * 4f, cellH * 4f))
            greenWells.forEach { drawCircle(Color(0xFF66BB6A), radius = wellRadius, center = it) }

            val yellowWells = listOf(Offset(cellW * 11f, cellH * 2f), Offset(cellW * 11f, cellH * 4f), Offset(cellW * 13f, cellH * 2f), Offset(cellW * 13f, cellH * 4f))
            yellowWells.forEach { drawCircle(Color(0xFFFFEE58), radius = wellRadius, center = it) }

            val blueWells = listOf(Offset(cellW * 11f, cellH * 11f), Offset(cellW * 11f, cellH * 13f), Offset(cellW * 13f, cellH * 11f), Offset(cellW * 13f, cellH * 13f))
            blueWells.forEach { drawCircle(Color(0xFF42A5F5), radius = wellRadius, center = it) }


            // --- Core Track Lanes ---
            val borderPaint = Stroke(width = 1.5f)

            for (r in 0..14) {
                for (c in 0..14) {
                    val isYard = (r <  6 && c <  6) || (r <  6 && c >  8) || (r >  8 && c <  6) || (r >  8 && c >  8)
                    val isCenter = (r in 6..8 && c in 6..8)

                    if (!isYard && !isCenter) {
                        // Determine custom cell colors
                        var cellColor = Color.White
                        
                        // Check Home Paths
                        if (r == 7 && c in 1..5) cellColor = Color(0xFFEF5350) // Red home lane
                        if (c == 7 && r in 1..5) cellColor = Color(0xFF66BB6A) // Green home lane
                        if (r == 7 && c in 9..13) cellColor = Color(0xFFFFEE58) // Yellow home lane
                        if (c == 7 && r in 9..13) cellColor = Color(0xFF42A5F5) // Blue home lane

                        // Check Spawns (Start cells)
                        if (r == 8 && c == 1) cellColor = Color(0xFFEF5350) // Red start
                        if (r == 1 && c == 6) cellColor = Color(0xFF66BB6A) // Green start
                        if (r == 6 && c == 13) cellColor = Color(0xFFFFEE58) // Yellow start
                        if (r == 13 && c == 8) cellColor = Color(0xFF42A5F5) // Blue start

                        val tLeft = Offset(c * cellW, r * cellH)
                        val cellSize = androidx.compose.ui.geometry.Size(cellW, cellH)

                        drawRect(color = cellColor, topLeft = tLeft, size = cellSize)
                        drawRect(color = Color(0xFF94A3B8), topLeft = tLeft, size = cellSize, style = borderPaint)
                    }
                }
            }

            // --- Home Victory Triangles (Center 3x3) ---
            val centerLeft = cellW * 6
            val centerTop = cellH * 6

            val pathRed = Path().apply {
                moveTo(centerLeft, centerTop)
                lineTo(centerLeft, centerTop + cellH * 3)
                lineTo(centerLeft + cellW * 1.5f, centerTop + cellH * 1.5f)
                close()
            }
            drawPath(path = pathRed, color = Color(0xFFEF5350), style = Fill)
            drawPath(path = pathRed, color = Color(0xFF1E293B), style = borderPaint)

            val pathGreen = Path().apply {
                moveTo(centerLeft, centerTop)
                lineTo(centerLeft + cellW * 3, centerTop)
                lineTo(centerLeft + cellW * 1.5f, centerTop + cellH * 1.5f)
                close()
            }
            drawPath(path = pathGreen, color = Color(0xFF66BB6A), style = Fill)
            drawPath(path = pathGreen, color = Color(0xFF1E293B), style = borderPaint)

            val pathYellow = Path().apply {
                moveTo(centerLeft + cellW * 3, centerTop)
                lineTo(centerLeft + cellW * 3, centerTop + cellH * 3)
                lineTo(centerLeft + cellW * 1.5f, centerTop + cellH * 1.5f)
                close()
            }
            drawPath(path = pathYellow, color = Color(0xFFFFEE58), style = Fill)
            drawPath(path = pathYellow, color = Color(0xFF1E293B), style = borderPaint)

            val pathBlue = Path().apply {
                moveTo(centerLeft, centerTop + cellH * 3)
                lineTo(centerLeft + cellW * 3, centerTop + cellH * 3)
                lineTo(centerLeft + cellW * 1.5f, centerTop + cellH * 1.5f)
                close()
            }
            drawPath(path = pathBlue, color = Color(0xFF42A5F5), style = Fill)
            drawPath(path = pathBlue, color = Color(0xFF1E293B), style = borderPaint)
        }

        // 2. Draw stars & decorative vector markers using Composables on top of Canvas
        for (r in 0..14) {
            for (c in 0..14) {
                val isSafe = LudoBoardHelper.safeCoordinates.contains(Coordinate(r, c))

                if (isSafe) {
                    val scaleX = maxWidth / 15f
                    val scaleY = maxHeight / 15f
                    Box(
                        modifier = Modifier
                            .size(scaleX, scaleY)
                            .offset(x = scaleX * c, y = scaleY * r),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Safe spot",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Draw Center Victory home icon
        Box(
            modifier = Modifier
                .size(maxWidth * 3 / 15f)
                .offset(x = maxWidth * 6 / 15f, y = maxHeight * 6 / 15f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Victory Circle",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF334155), CircleShape)
                    .padding(4.dp)
            )
        }

        // 3. Draw game tokens dynamically (supporting stacking visually!)
        tokensByCoordinate.forEach { (coord, occupyingTokens) ->
            val cellW = maxWidth / 15f
            val cellH = maxHeight / 15f

            Box(
                modifier = Modifier
                    .size(cellW, cellH)
                    .offset(x = cellW * coord.col, y = cellH * coord.row),
                contentAlignment = Alignment.Center
            ) {
                if (occupyingTokens.size == 1) {
                    val token = occupyingTokens.first()
                    val isSelectable = validSelectableTokens.any { it.color == token.color && it.id == token.id }

                    TokenPill(
                        token = token,
                        isSelectable = isSelectable,
                        isActiveTurn = token.color == activePlayer,
                        onClick = { onTokenClick(token) }
                    )
                } else {
                    // Draw multiple tokens elegantly side by side or layered!
                    // Let's create an overlapping wrap for stacked tokens
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Display the token count overlay badge
                        val isAnySelectable = occupyingTokens.any { t ->
                            validSelectableTokens.any { st -> st.color == t.color && st.id == t.id }
                        }

                        // Just pick primary token or show stacked badges
                        val primaryToken = occupyingTokens.firstOrNull { t ->
                            validSelectableTokens.any { st -> st.color == t.color && st.id == t.id }
                        } ?: occupyingTokens.first()

                        TokenPill(
                            token = primaryToken,
                            isSelectable = isAnySelectable,
                            isActiveTurn = primaryToken.color == activePlayer,
                            onClick = {
                                // If any token of active player is clickable, move that one!
                                val selectable = occupyingTokens.firstOrNull { t ->
                                    validSelectableTokens.any { st -> st.color == t.color && st.id == t.id }
                                }
                                if (selectable != null) {
                                    onTokenClick(selectable)
                                } else {
                                    onTokenClick(primaryToken)
                                }
                            },
                            badgeCount = occupyingTokens.size
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TokenPill(
    token: LudoToken,
    isSelectable: Boolean,
    isActiveTurn: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 1,
    modifier: Modifier = Modifier
) {
    val tokenColor = token.color.color
    val pulseModifier = if (isSelectable) {
        Modifier
            .border(2.dp, Color.White, CircleShape)
            .border(4.dp, Color(0xFFFFD700), CircleShape) // Gold glow for selectable tokens!
            .shadow(4.dp, CircleShape)
    } else {
        Modifier
            .border(1.5.dp, Color.White, CircleShape)
            .shadow(1.dp, CircleShape)
    }

    Box(
        modifier = modifier
            .fillMaxSize(0.85f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(tokenColor.copy(alpha = 0.9f), tokenColor)
                )
            )
            .then(pulseModifier)
            .clickable(enabled = isSelectable) { onClick() }
            .testTag("token_${token.color.name}_${token.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Token visual ID and badge counter
            Text(
                text = if (badgeCount > 1) "x$badgeCount" else "${token.id + 1}",
                color = if (token.color == LudoColor.YELLOW) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (badgeCount > 1) 10.sp else 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Local helper to simplify density conversions
private val LocalDeviceDensity = 1.0f
