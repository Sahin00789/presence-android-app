package com.als.presence.data.local.wallet

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_summary")
data class WalletEntity(
    @PrimaryKey val staffProfileId: String,
    val earnedBalanceCents: Long,
    val approvedUnpaidBalanceCents: Long,
    val paidBalanceCents: Long,
    val totalBalanceCents: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
