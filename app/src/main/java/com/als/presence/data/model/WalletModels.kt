package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WalletSummaryDto(
    val staffProfileId: String,
    val earnedBalanceCents: Long,
    val approvedUnpaidBalanceCents: Long,
    val paidBalanceCents: Long,
    val totalBalanceCents: Long
)

@Serializable
data class WalletTransactionDto(
    val id: String,
    val walletId: String,
    val type: String,
    val amountCents: Long,
    val balanceBucket: String,
    val idempotencyKey: String,
    val referenceId: String? = null,
    val description: String? = null,
    val createdAt: String
)
