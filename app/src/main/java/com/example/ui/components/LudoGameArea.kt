package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.*

@Composable
fun LudoGameArea(
    gameState: LudoGameState,
    currentSkin: DiceSkin,
    validSelectableTokens: List<LudoToken>,
    onRollClick: () -> Unit,
    onTokenSelect: (LudoToken) -> Unit,
    onRestartClick: (Int) -> Unit,
    onPowerUpUse: (PowerUpType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            // 1. Player configuration & setup toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Config
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Config",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Game Mode:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { onRestartClick(2) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gameState.playerCount == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (gameState.playerCount == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("mode_2_player"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("2 Players", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onRestartClick(4) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gameState.playerCount == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (gameState.playerCount == 4) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("mode_4_player"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("4 Players", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. Active turn indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(gameState.currentTurnPlayer.color.copy(alpha = 0.15f))
                    .border(1.dp, gameState.currentTurnPlayer.color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(gameState.currentTurnPlayer.color, CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Player: ${gameState.currentTurnPlayer.displayName}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (gameState.isGameOver) {
                    Text(
                        text = "🏆 GAME COMPLETED!",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC2410C),
                        fontSize = 12.sp
                    )
                } else if (gameState.isAutoMoving) {
                    Text(
                        text = "Tokens sliding...",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = if (gameState.hasRolledThisTurn) "Select piece to move" else "Roll the dice!",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }
            }

            // 3. Interactive Custom-Styled Dice Roller Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Custom Dice Drawing
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Theme: ${currentSkin.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    PhysicalDice(
                        value = gameState.diceValue,
                        skin = currentSkin,
                        isRolling = gameState.isAutoMoving,
                        enabled = !gameState.hasRolledThisTurn && !gameState.isGameOver && !gameState.isAutoMoving,
                        onClick = onRollClick
                    )
                }

                // Power up triggers panel
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Use special power-up roll:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PowerUpType.entries.forEach { power ->
                            val isUsed = gameState.activePowerUpUsed == power
                            val isAnyPowerUpActive = gameState.activePowerUpUsed != null

                            Button(
                                onClick = { onPowerUpUse(power) },
                                enabled = !gameState.hasRolledThisTurn && !gameState.isGameOver && !gameState.isAutoMoving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isUsed) Color(0xFFCA8A04) else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isUsed) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .testTag("use_${power.id}")
                            ) {
                                Text(
                                    text = power.displayName, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    if (gameState.activePowerUpUsed != null) {
                        Text(
                            text = "✨ Active Roll: ${gameState.activePowerUpUsed?.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA16207),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Compact Touchable Selectable Items Panel (A11y Touch-Targets)
            if (gameState.hasRolledThisTurn && validSelectableTokens.isNotEmpty() && !gameState.isAutoMoving) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "👇 Tap piece below to move:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        validSelectableTokens.forEach { token ->
                            Button(
                                onClick = { onTokenSelect(token) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("action_token_${token.id}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = token.color.color
                                )
                            ) {
                                Text(
                                    text = if (token.isHomeYard) "Release Piece ${token.id + 1}" else "Move Piece ${token.id + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (token.color == LudoColor.YELLOW) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 5. Game Historic Log console (Shows captures, listings, payouts)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "🖥️ SYSTEM LIVE CONSOLE",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    gameState.turnMessages.takeLast(5).forEach { log ->
                        Text(
                            text = log,
                            color = if (log.contains("rolled") || log.contains("purchased") || log.contains("WINS")) Color(0xFF34D399) else if (log.contains("captured") || log.contains("❌")) Color(0xFFF87171) else Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhysicalDice(
    value: Int?,
    skin: DiceSkin,
    isRolling: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(skin.primaryColor)
            .border(2.dp, Color.White, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .testTag("dice_roller"),
        contentAlignment = Alignment.Center
    ) {
        if (value == null) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = "Dice roll",
                tint = skin.pipColor,
                modifier = Modifier.size(32.dp)
            )
        } else {
            // Precise Canvas Placement of Dice Dots/Pips
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val pipRad = size.width * 0.11f
                val leftCol = size.width * 0.2f
                val midCol = size.width * 0.5f
                val rightCol = size.width * 0.8f
                
                val topRow = size.height * 0.2f
                val midRow = size.height * 0.5f
                val botRow = size.height * 0.8f

                when (value) {
                    1 -> {
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(midCol, midRow))
                    }
                    2 -> {
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, topRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, botRow))
                    }
                    3 -> {
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, topRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(midCol, midRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, botRow))
                    }
                    4 -> {
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, topRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, topRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, botRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, botRow))
                    }
                    5 -> {
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, topRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, topRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(midCol, midRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, botRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, botRow))
                    }
                    6 -> {
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, topRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, topRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, midRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, midRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(leftCol, botRow))
                        drawCircle(skin.pipColor, radius = pipRad, center = Offset(rightCol, botRow))
                    }
                }
            }
        }
    }
}
