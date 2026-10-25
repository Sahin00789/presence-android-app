package com.als.presence.data.local.shift

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_assignments")
data class ShiftAssignmentEntity(
    @PrimaryKey val id: String,
    val staffProfileId: String,
    val shiftTemplateId: String,
    val premisesId: String? = null,
    val staffName: String,
    val shiftName: String,
    val startDate: String,
    val endDate: String? = null,
    val paymentCents: Long? = null,
    val daysOfWeekJson: String? = null,
    val lateClockInAllowed: Boolean = true
)
