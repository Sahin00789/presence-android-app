package com.als.presence.data.local.wallet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: WalletEntity)

    @Query("SELECT * FROM wallet_summary WHERE staffProfileId = :staffProfileId LIMIT 1")
    fun observe(staffProfileId: String): Flow<WalletEntity?>

    @Query("SELECT * FROM wallet_summary WHERE staffProfileId = :staffProfileId LIMIT 1")
    suspend fun get(staffProfileId: String): WalletEntity?

    @Query("DELETE FROM wallet_summary")
    suspend fun clear()
}
