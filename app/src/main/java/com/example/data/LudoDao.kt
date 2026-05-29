package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LudoDao {
    @Query("SELECT * FROM ludo_profiles WHERE id = 0")
    fun getProfileFlow(): Flow<LudoProfile?>

    @Query("SELECT * FROM ludo_profiles WHERE id = 0")
    suspend fun getProfileDirect(): LudoProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: LudoProfile)
}
