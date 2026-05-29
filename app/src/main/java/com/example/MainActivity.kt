package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LudoViewModel
import com.example.ui.components.DiceShop
import com.example.ui.components.LudoBoard
import com.example.ui.components.LudoGameArea
import com.example.ui.models.DiceSkinData
import com.example.ui.models.PowerUpType
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = Color(0xFF0F172A) // Sleek Premium Cosmic Dark Theme
                ) { innerPadding ->
                    LudoExchangeApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoExchangeApp(
    modifier: Modifier = Modifier,
    viewModel: LudoViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()

    val currentSkin = remember(profile.activeSkinId) {
        DiceSkinData.skins.find { it.id == profile.activeSkinId } ?: DiceSkinData.skins.first()
    }

    val validSelectableTokens = remember(gameState) {
        gameState.diceValue?.let { viewModel.getValidMovesForDice(it) } ?: emptyList()
    }

    val scrollState = rememberScrollState()

    // 1. Pick-A-Dice Dialog
    if (viewModel.showValuePicker.value) {
        AlertDialog(
            onDismissRequest = { /* Maintain modal state to ensure correct game choice flow */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Pick Roll",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Luck Picker Activated!",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Text(
                    text = "You paid for this privilege! Select the exact value you want to force for this roll:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..6).forEach { value ->
                        Button(
                            onClick = { viewModel.selectDiceValueManually(value) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("pick_value_$value"),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentSkin.primaryColor,
                                contentColor = currentSkin.pipColor
                            )
                        ) {
                            Text(
                                text = "$value",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        )
    }

    // 2. Core Layout
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        // Header Banner with Wallet Score
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF334155))
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ludo 🎲",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Earn points & exchange for customized dice!",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Stats Chip
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Stats",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "High: 🪙 ${profile.highScore}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Ludo board view
        LudoBoard(
            tokens = gameState.tokens,
            activePlayer = gameState.currentTurnPlayer,
            validSelectableTokens = validSelectableTokens,
            onTokenClick = { viewModel.selectTokenToMove(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // Rolling gameplay dashboard console
        LudoGameArea(
            gameState = gameState,
            currentSkin = currentSkin,
            validSelectableTokens = validSelectableTokens,
            onRollClick = { viewModel.rollDice() },
            onTokenSelect = { viewModel.selectTokenToMove(it) },
            onRestartClick = { count -> viewModel.restartGame(count) },
            onPowerUpUse = { powerUp -> viewModel.activatePowerUp(powerUp) }
        )

        // Dice exchange & Shop view
        DiceShop(
            profile = profile,
            onBuySkin = { skin -> viewModel.purchaseSkin(skin) },
            onEquipSkin = { skinId -> viewModel.equipSkin(skinId) },
            onBuyPowerUp = { power -> viewModel.purchasePowerUp(power) }
        )

        // Informative Guideline Card at Bottom
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "How to play",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Payout Rules & Mechanics:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "• Move tokens along the track to earn 🪙 +2 points per cell.\n" +
                           "• Rolling a lucky '6' unlocks locked home tokens onto start and gives 🪙 +10.\n" +
                           "• Capturing opponent tokens awards 🪙 +80 points!\n" +
                           "• Landing a token in HOME adds a massive 🪙 +100 points!\n" +
                           "• Premium Dice skins multiply all game payouts (e.g. Cosmic fire gives +30% score bonus permanently!).\n" +
                           "• Purchase Power-up rolls (e.g. Guaranteed 6, Lucky Pickers) inside the exchange shop to manipulate rolls at crucial moments!",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
