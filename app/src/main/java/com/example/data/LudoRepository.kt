package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LudoRepository(private val ludoDao: LudoDao) {
    
    val profileFlow: Flow<LudoProfile> = ludoDao.getProfileFlow().map { profile ->
        profile ?: LudoProfile()
    }

    suspend fun getProfile(): LudoProfile = withContext(Dispatchers.IO) {
        var profile = ludoDao.getProfileDirect()
        if (profile == null) {
            profile = LudoProfile()
            ludoDao.insertOrUpdate(profile)
        }
        profile
    }

    suspend fun updateProfile(profile: LudoProfile) = withContext(Dispatchers.IO) {
        ludoDao.insertOrUpdate(profile)
    }

    suspend fun addScore(amount: Int) {
        val current = getProfile()
        val newScore = current.walletScore + amount
        val newHighScore = maxOf(current.highScore, newScore)
        updateProfile(current.copy(walletScore = newScore, highScore = newHighScore))
    }

    suspend fun spendScore(amount: Int): Boolean {
        val current = getProfile()
        if (current.walletScore >= amount) {
            updateProfile(current.copy(walletScore = current.walletScore - amount))
            return true
        }
        return false
    }
}
