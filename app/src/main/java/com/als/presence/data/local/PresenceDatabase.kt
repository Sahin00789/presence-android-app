package com.als.presence.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.als.presence.data.local.attendance.AttendanceDao
import com.als.presence.data.local.attendance.AttendanceEntity
import com.als.presence.data.local.location.LocationCheckDao
import com.als.presence.data.local.location.LocationCheckEntity
import com.als.presence.data.local.notification.NotificationDao
import com.als.presence.data.local.notification.NotificationEntity
import com.als.presence.data.local.premises.PremisesDao
import com.als.presence.data.local.premises.PremisesEntity
import com.als.presence.data.local.shift.ShiftAssignmentEntity
import com.als.presence.data.local.shift.ShiftDao
import com.als.presence.data.local.shift.ShiftTemplateEntity
import com.als.presence.data.local.staff.StaffDao
import com.als.presence.data.local.staff.StaffEntity
import com.als.presence.data.local.wallet.WalletDao
import com.als.presence.data.local.wallet.WalletEntity

@Database(
    entities = [
        NotificationEntity::class,
        LocationCheckEntity::class,
        WalletEntity::class,
        AttendanceEntity::class,
        ShiftTemplateEntity::class,
        ShiftAssignmentEntity::class,
        PremisesEntity::class,
        StaffEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PresenceDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun locationCheckDao(): LocationCheckDao
    abstract fun walletDao(): WalletDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun shiftDao(): ShiftDao
    abstract fun premisesDao(): PremisesDao
    abstract fun staffDao(): StaffDao
}
