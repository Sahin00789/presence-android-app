package com.als.presence.data.local.staff

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val email: String,
    val fullName: String,
    val staffCode: String,
    val department: String? = null,
    val designation: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null,
    val hireDate: String,
    val isActive: Boolean,
    val compensationType: String? = null,
    val baseRateCents: Long? = null,
    val overtimeRateCents: Long? = null,
    val earnedBalanceCents: Long = 0,
    val approvedUnpaidBalanceCents: Long = 0,
    val paidBalanceCents: Long = 0,
    val totalBalanceCents: Long = 0
)
