package com.als.presence.data.local.shift

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_templates")
data class ShiftTemplateEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val premisesId: String? = null,
    val name: String,
    val startTime: String,
    val endTime: String,
    val breakMinutes: Int,
    val lateGraceMinutes: Int,
    val halfDayThresholdMinutes: Int,
    val daysOfWeekJson: String? = null,
    val lateClockInAllowed: Boolean = true,
    val overnight: Boolean = false,
    val isActive: Boolean = true
)
