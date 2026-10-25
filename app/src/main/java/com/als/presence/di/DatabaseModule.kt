package com.als.presence.di

import android.content.Context
import androidx.room.Room
import com.als.presence.data.local.PresenceDatabase
import com.als.presence.data.local.attendance.AttendanceDao
import com.als.presence.data.local.location.LocationCheckDao
import com.als.presence.data.local.notification.NotificationDao
import com.als.presence.data.local.premises.PremisesDao
import com.als.presence.data.local.shift.ShiftDao
import com.als.presence.data.local.staff.StaffDao
import com.als.presence.data.local.wallet.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PresenceDatabase {
        return Room.databaseBuilder(
            context,
            PresenceDatabase::class.java,
            "presence.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(db: PresenceDatabase): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun provideLocationCheckDao(db: PresenceDatabase): LocationCheckDao = db.locationCheckDao()

    @Provides
    @Singleton
    fun provideWalletDao(db: PresenceDatabase): WalletDao = db.walletDao()

    @Provides
    @Singleton
    fun provideAttendanceDao(db: PresenceDatabase): AttendanceDao = db.attendanceDao()

    @Provides
    @Singleton
    fun provideShiftDao(db: PresenceDatabase): ShiftDao = db.shiftDao()

    @Provides
    @Singleton
    fun providePremisesDao(db: PresenceDatabase): PremisesDao = db.premisesDao()

    @Provides
    @Singleton
    fun provideStaffDao(db: PresenceDatabase): StaffDao = db.staffDao()
}
