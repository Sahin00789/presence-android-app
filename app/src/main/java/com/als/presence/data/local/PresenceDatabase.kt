package com.als.presence.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        NotificationEntity::class,
        LocationCheckEntity::class,
        AttendanceEntity::class,
        ShiftEntity::class,
        PremisesEntity::class,
        EmployeeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PresenceDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun locationCheckDao(): LocationCheckDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun shiftDao(): ShiftDao
    abstract fun premisesDao(): PremisesDao
    abstract fun employeeDao(): EmployeeDao
}
