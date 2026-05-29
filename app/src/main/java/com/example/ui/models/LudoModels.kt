package com.example.ui.models

import androidx.compose.ui.graphics.Color

enum class LudoColor(val displayName: String, val color: Color) {
    RED("Red", Color(0xFFEF5350)),
    GREEN("Green", Color(0xFF66BB6A)),
    YELLOW("Yellow", Color(0xFFFFEE58)),
    BLUE("Blue", Color(0xFF42A5F5))
}

data class LudoToken(
    val id: Int,               // 0 to 3
    val color: LudoColor,       // RED, GREEN, YELLOW, BLUE
    val relativePosition: Int = 0 // 0 = Home Yard, 1 = Start cell, 2..51 = track cells, 52..56 = home path, 57 = completed!
) {
    val isHomeYard: Boolean get() = relativePosition == 0
    val isCompleted: Boolean get() = relativePosition == 57
    val isInHomePath: Boolean get() = relativePosition in 52..56
    val isOnTrack: Boolean get() = relativePosition in 1..51
}

enum class PowerUpType(val id: String, val displayName: String, val description: String, val cost: Int) {
    GUARANTEED_6("guaranteed_6", "Lucky 6", "Forces output to be a 6!", 80),
    EXTRA_TURN("extra_turn", "Extra Turn", "Grants another turn!", 100),
    CHOOSE_ROLL("choose_roll", "Pick-a-Dice", "Choose value 1-6 manually!", 120)
}

data class DiceSkin(
    val id: String,
    val displayName: String,
    val cost: Int,
    val isPremium: Boolean,
    val primaryColorLong: Long, // Use Long value for serializability/simplicity
    val pipColorLong: Long,
    val bonusMultiplier: Float = 1.0f
) {
    val primaryColor: Color get() = Color(primaryColorLong)
    val pipColor: Color get() = Color(pipColorLong)
}

data class LudoGameState(
    val playerCount: Int = 2, // 2 or 4 players
    val activePlayers: List<LudoColor> = listOf(LudoColor.RED, LudoColor.BLUE),
    val currentTurnIndex: Int = 0,
    val tokens: List<LudoToken> = emptyList(),
    val diceValue: Int? = null,
    val hasRolledThisTurn: Boolean = false,
    val isGameOver: Boolean = false,
    val winner: LudoColor? = null,
    val consecutiveSixes: Int = 0,
    val movesAvailable: Boolean = false,
    val isAutoMoving: Boolean = false,
    val activePowerUpUsed: PowerUpType? = null,
    val doubleRollUsedThisTurn: Boolean = false,
    val turnMessages: List<String> = listOf("Welcome to Ludo! Roll some dice to start.")
) {
    val currentTurnPlayer: LudoColor get() = activePlayers[currentTurnIndex]
}

object DiceSkinData {
    val skins = listOf(
        DiceSkin("classic", "Classic Red", 0, false, 0xFFEF5350, 0xFFFFFFFF),
        DiceSkin("neon", "Neon Ocean", 150, true, 0xFF00E5FF, 0xFF0F172A),
        DiceSkin("emerald", "Emerald Grass", 300, true, 0xFF00E676, 0xFF1B5E20),
        DiceSkin("gold", "Golden Fortune (+15% Score)", 600, true, 0xFFFFD700, 0xFFE65100, 1.15f),
        DiceSkin("cosmic", "Cosmic Fire (+30% Score)", 1000, true, 0xFFD500F9, 0xFFFFFFFF, 1.30f)
    )
}
