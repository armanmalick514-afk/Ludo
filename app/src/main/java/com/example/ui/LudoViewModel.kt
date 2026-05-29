package com.example.ui

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LudoDatabase
import com.example.data.LudoProfile
import com.example.data.LudoRepository
import com.example.ui.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LudoDatabase.getDatabase(application)
    private val repository = LudoRepository(database.ludoDao())

    // Room Profile State Flow
    private val _playerProfile = MutableStateFlow(LudoProfile())
    val playerProfile: StateFlow<LudoProfile> = _playerProfile.asStateFlow()

    // Ludo Game State
    private val _gameState = MutableStateFlow(LudoGameState())
    val gameState: StateFlow<LudoGameState> = _gameState.asStateFlow()

    // Manual roll picker (for the Pick-A-Dice powerup)
    var showValuePicker = mutableStateOf(false)
        private set

    init {
        // Safe database seeding on Dispatchers.IO via Repository
        viewModelScope.launch {
            repository.getProfile()
        }

        // Observe local database profile updates
        viewModelScope.launch {
            repository.profileFlow.collectLatest { profile ->
                _playerProfile.value = profile
            }
        }
        
        // Start a default 2-player game
        restartGame(playerCount = 2)
    }

    // --- GAME ENGINE ---

    fun restartGame(playerCount: Int) {
        val players = if (playerCount == 2) {
            listOf(LudoColor.RED, LudoColor.BLUE)
        } else {
            listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
        }

        // 4 tokens per player
        val initialTokens = players.flatMap { color ->
            (0..3).map { id -> LudoToken(id = id, color = color) }
        }

        _gameState.value = LudoGameState(
            playerCount = playerCount,
            activePlayers = players,
            currentTurnIndex = 0,
            tokens = initialTokens,
            diceValue = null,
            hasRolledThisTurn = false,
            isGameOver = false,
            winner = null,
            consecutiveSixes = 0,
            movesAvailable = false,
            isAutoMoving = false,
            activePowerUpUsed = null,
            doubleRollUsedThisTurn = false,
            turnMessages = listOf("New Ludo Game Started! ${players.size} players ready. Red starts first!")
        )
    }

    // Spend score to activate a power-up for the current roll
    fun activatePowerUp(powerUp: PowerUpType) {
        val profile = _playerProfile.value
        val matchesCount = when (powerUp) {
            PowerUpType.GUARANTEED_6 -> profile.goldenRollCount
            PowerUpType.EXTRA_TURN -> profile.extraTurnCount
            PowerUpType.CHOOSE_ROLL -> profile.chooseRollCount
        }

        if (matchesCount <= 0) {
            addTurnLog("❌ You don't own any ${powerUp.displayName}! Buy them in the Exchange shop below.")
            return
        }

        if (_gameState.value.hasRolledThisTurn) {
            addTurnLog("You have already rolled this turn!")
            return
        }

        if (_gameState.value.activePowerUpUsed != null) {
            addTurnLog("A powerup is already active!")
            return
        }

        viewModelScope.launch {
            // Deduct the inventory count
            val updated = when (powerUp) {
                PowerUpType.GUARANTEED_6 -> profile.copy(goldenRollCount = profile.goldenRollCount - 1)
                PowerUpType.EXTRA_TURN -> profile.copy(extraTurnCount = profile.extraTurnCount - 1)
                PowerUpType.CHOOSE_ROLL -> profile.copy(chooseRollCount = profile.chooseRollCount - 1)
            }
            repository.updateProfile(updated)

            _gameState.value = _gameState.value.copy(activePowerUpUsed = powerUp)
            addTurnLog("✨ Activated powerup: ${powerUp.displayName}! (${powerUp.description})")

            // If Pick-a-value is activated, trigger selection UI
            if (powerUp == PowerUpType.CHOOSE_ROLL) {
                showValuePicker.value = true
            }
        }
    }

    // Choose dice value manually (Pick-A-Dice Powerup result)
    fun selectDiceValueManually(value: Int) {
        showValuePicker.value = false
        executeRollWithDiceValue(value)
    }

    // Roll dice trigger
    fun rollDice() {
        val state = _gameState.value
        if (state.hasRolledThisTurn || state.isGameOver || state.isAutoMoving) return

        // Compute roll value
        val rollValue = when (state.activePowerUpUsed) {
            PowerUpType.GUARANTEED_6 -> 6
            PowerUpType.CHOOSE_ROLL -> {
                // Already handled or currently pending selection
                return
            }
            else -> Random.nextInt(1, 7)
        }

        executeRollWithDiceValue(rollValue)
    }

    // Common execution of roll
    private fun executeRollWithDiceValue(rollValue: Int) {
        val state = _gameState.value
        val player = state.currentTurnPlayer

        var consSixes = state.consecutiveSixes
        if (rollValue == 6) {
            consSixes += 1
        } else {
            consSixes = 0
        }

        addTurnLog("🎲 $player rolled a $rollValue!")

        // If 3 consecutive sixes, turn ends immediately!
        if (consSixes == 3) {
            addTurnLog("⚠️ 3 consecutive sixes! Turn passes to next player.")
            viewModelScope.launch {
                _gameState.value = state.copy(
                    diceValue = rollValue,
                    hasRolledThisTurn = true,
                    consecutiveSixes = 0
                )
                delay(1200)
                endTurn()
            }
            return
        }

        // Check valid moves
        val validTokens = getValidMovesForDice(rollValue)
        val hasMoves = validTokens.isNotEmpty()

        _gameState.value = _gameState.value.copy(
            diceValue = rollValue,
            hasRolledThisTurn = true,
            consecutiveSixes = consSixes,
            movesAvailable = hasMoves
        )

        if (rollValue == 6) {
            earnPoints(10) // Small score reward for rolling a 6!
        }

        // If no moves, auto skip turn after delay
        if (!hasMoves) {
            addTurnLog("No moves available for $player. Passing turn...")
            viewModelScope.launch {
                delay(1500)
                endTurn()
            }
        }
    }

    // Find valid tokens for active player
    fun getValidMovesForDice(dice: Int): List<LudoToken> {
        val state = _gameState.value
        val activePlayer = state.currentTurnPlayer
        val activeTokens = state.tokens.filter { it.color == activePlayer }
        return activeTokens.filter { isValidMove(it, dice) }
    }

    fun isValidMove(token: LudoToken, dice: Int): Boolean {
        if (token.isCompleted) return false
        if (token.isHomeYard) {
            return dice == 6
        }
        return token.relativePosition + dice <= 57
    }

    // Token clicked to move
    fun selectTokenToMove(token: LudoToken) {
        val state = _gameState.value
        val dice = state.diceValue
        if (dice == null || !state.hasRolledThisTurn || state.isAutoMoving || state.isGameOver) return

        if (!isValidMove(token, dice)) {
            addTurnLog("⚠️ Invalid move for this token.")
            return
        }

        // Start step-by-step rolling movement!
        viewModelScope.launch {
            _gameState.value = _gameState.value.copy(isAutoMoving = true)

            var currentToken = token
            val sourcePos = token.relativePosition
            val targetPos = if (sourcePos == 0 && dice == 6) 1 else sourcePos + dice
            var accumulatedPoints = 0

            if (sourcePos == 0) {
                // Spawn token onto track start cell
                currentToken = currentToken.copy(relativePosition = 1)
                updateStateToken(currentToken)
                accumulatedPoints += 20 // Release bonus points!
                delay(200)
            } else {
                // Hop step by step
                for (pos in (sourcePos + 1)..targetPos) {
                    currentToken = currentToken.copy(relativePosition = pos)
                    updateStateToken(currentToken)
                    accumulatedPoints += 2
                    delay(120)
                }
            }

            // Earn all accumulated movement points in a single database transaction
            if (accumulatedPoints > 0) {
                earnPoints(accumulatedPoints)
            }

            // Move completed! Check for captures or home reached
            onTokenMoveFinished(currentToken)
        }
    }

    private fun updateStateToken(updatedToken: LudoToken) {
        val currentTokens = _gameState.value.tokens.toMutableList()
        val index = currentTokens.indexOfFirst { it.color == updatedToken.color && it.id == updatedToken.id }
        if (index != -1) {
            currentTokens[index] = updatedToken
        }
        _gameState.value = _gameState.value.copy(tokens = currentTokens)
    }

    private suspend fun onTokenMoveFinished(arrivedToken: LudoToken) {
        var state = _gameState.value
        val player = arrivedToken.color
        val dice = state.diceValue ?: 6

        var nextRollBonus = false

        if (arrivedToken.isCompleted) {
            earnPoints(100) // Huge points for getting home!
            addTurnLog("🏠 Token ${arrivedToken.id + 1} of $player reached HOME! (+100 score)")
            nextRollBonus = true // Reaching home gives another roll!
        } else {
            // Check for capture on track
            val boardCoord = LudoBoardHelper.getCoordinate(arrivedToken)
            val isSafeZone = LudoBoardHelper.trackCells.indexOf(boardCoord).let { index ->
                index != -1 && LudoBoardHelper.safeIndices.contains(index)
            }

            if (!isSafeZone) {
                // Find opponent tokens sharing same coordinate
                val opponentTokens = state.tokens.filter { 
                    it.color != player && 
                    !it.isHomeYard && 
                    !it.isCompleted && 
                    LudoBoardHelper.getCoordinate(it) == boardCoord 
                }

                if (opponentTokens.isNotEmpty()) {
                    // Capture all sharing opponent tokens!
                    opponentTokens.forEach { victim ->
                        val resetVictim = victim.copy(relativePosition = 0)
                        updateStateToken(resetVictim)
                        addTurnLog("⚔️ $player captured ${victim.color}'s Token ${victim.id + 1}! Sent back to yard.")
                    }
                    val capturePoints = 80 * opponentTokens.size
                    earnPoints(capturePoints) // Capture points!
                    nextRollBonus = true // Capturing gives another roll!
                }
            }
        }

        // Check for Game Over: If a player gets all 4 tokens home!
        val activePlayerTokens = _gameState.value.tokens.filter { it.color == player }
        val allHome = activePlayerTokens.all { it.isCompleted }

        if (allHome) {
            earnPoints(250) // Win bonus points!
            addTurnLog("🏆🏆 $player WINS LUDO EXCHANGE! 🏆🏆 (+250 completion bonus)")
            _gameState.value = _gameState.value.copy(
                isGameOver = true,
                winner = player,
                isAutoMoving = false
            )
            return
        }

        // Determine who gets the next turn
        val rolledSix = (dice == 6)
        val extraTurnPowerUp = (state.activePowerUpUsed == PowerUpType.EXTRA_TURN)
        
        val getsAnotherRoll = rolledSix || nextRollBonus || extraTurnPowerUp

        if (getsAnotherRoll) {
            addTurnLog("✨ $player gets an extra roll!")
            _gameState.value = _gameState.value.copy(
                diceValue = null,
                hasRolledThisTurn = false,
                isAutoMoving = false,
                activePowerUpUsed = null // Reset powerup
            )
        } else {
            endTurn()
        }
    }

    private fun endTurn() {
        val state = _gameState.value
        val nextIndex = (state.currentTurnIndex + 1) % state.activePlayers.size
        val nextPlayer = state.activePlayers[nextIndex]
        
        _gameState.value = state.copy(
            currentTurnIndex = nextIndex,
            diceValue = null,
            hasRolledThisTurn = false,
            isAutoMoving = false,
            activePowerUpUsed = null
        )
        addTurnLog("Turn passes to $nextPlayer.")
    }

    private fun addTurnLog(msg: String) {
        val currentLogs = _gameState.value.turnMessages.toMutableList()
        currentLogs.add(0, msg) // Add at start of list (newest first)
        if (currentLogs.size > 25) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _gameState.value = _gameState.value.copy(turnMessages = currentLogs)
    }

    // --- SERVICE LAYER: SCORE EARNINGS & MULTIPLIERS ---

    private fun earnPoints(rawAmount: Int) {
        viewModelScope.launch {
            val activeSkinId = _playerProfile.value.activeSkinId
            val activeSkin = DiceSkinData.skins.find { it.id == activeSkinId } ?: DiceSkinData.skins.first()
            val multiplier = activeSkin.bonusMultiplier

            val finalAmount = (rawAmount * multiplier).toInt()
            repository.addScore(finalAmount)
        }
    }

    // --- GAME SHOP & SCORE EXCHANGE ---

    fun purchaseSkin(skin: DiceSkin) {
        viewModelScope.launch {
            val profile = _playerProfile.value
            if (profile.unlockedSkins.contains(skin.id)) {
                addTurnLog("You already own ${skin.displayName}!")
                return@launch
            }

            if (repository.spendScore(skin.cost)) {
                // Successfully bought
                val updatedSkins = "${profile.unlockedSkins},${skin.id}"
                repository.updateProfile(profile.copy(
                    unlockedSkins = updatedSkins,
                    activeSkinId = skin.id // Auto equip
                ))
                addTurnLog("🛍️ Purchased & Equipped ${skin.displayName} dice skin!")
            } else {
                addTurnLog("❌ Insufficient points! Roll more on the board to earn score.")
            }
        }
    }

    fun equipSkin(skinId: String) {
        viewModelScope.launch {
            val profile = _playerProfile.value
            if (!profile.unlockedSkins.contains(skinId)) {
                addTurnLog("Unlock skin before equipping.")
                return@launch
            }

            repository.updateProfile(profile.copy(activeSkinId = skinId))
            val skinName = DiceSkinData.skins.find { it.id == skinId }?.displayName ?: skinId
            addTurnLog("Equipped $skinName style!")
        }
    }

    fun purchasePowerUp(powerUp: PowerUpType) {
        viewModelScope.launch {
            val profile = _playerProfile.value
            if (repository.spendScore(powerUp.cost)) {
                val updated = when (powerUp) {
                    PowerUpType.GUARANTEED_6 -> profile.copy(goldenRollCount = profile.goldenRollCount + 1)
                    PowerUpType.EXTRA_TURN -> profile.copy(extraTurnCount = profile.extraTurnCount + 1)
                    PowerUpType.CHOOSE_ROLL -> profile.copy(chooseRollCount = profile.chooseRollCount + 1)
                }
                repository.updateProfile(updated)
                addTurnLog("🛒 Exchanged points for 1x ${powerUp.displayName}!")
            } else {
                addTurnLog("❌ Insufficient score for this power-up!")
            }
        }
    }
}
