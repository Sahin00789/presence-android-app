package com.als.presence.data.local.attendance

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val staffProfileId: String,
    val staffName: String? = null,
    val staffEmail: String? = null,
    val staffCode: String? = null,
    val workDate: String? = null,
    val clockIn: String? = null,
    val clockOut: String? = null,
    val status: String,
    val faceVerified: Boolean = false,
    val geofenceVerified: Boolean = false,
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val scheduledMinutes: Int = 0,
    val premisesId: String? = null,
    val overtimeId: String? = null
)
