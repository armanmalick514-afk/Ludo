package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ludo_profiles")
data class LudoProfile(
    @PrimaryKey val id: Int = 0,
    val walletScore: Int = 150, // Give them 150 start credits to spend in the shop immediately!
    val highScore: Int = 0,
    val unlockedSkins: String = "classic",
    val activeSkinId: String = "classic",
    val goldenRollCount: Int = 1,
    val extraTurnCount: Int = 1,
    val chooseRollCount: Int = 1
)
