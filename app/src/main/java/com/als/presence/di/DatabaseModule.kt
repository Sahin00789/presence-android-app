package com.als.presence.di

import android.content.Context
import androidx.room.Room
import com.als.presence.data.local.*
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
    fun provideUserDao(db: PresenceDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideOrganizationDao(db: PresenceDatabase): OrganizationDao = db.organizationDao()

    @Provides
    @Singleton
    fun provideNotificationDao(db: PresenceDatabase): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun provideLocationCheckDao(db: PresenceDatabase): LocationCheckDao = db.locationCheckDao()

    @Provides
    @Singleton
    fun provideAttendanceDao(db: PresenceDatabase): AttendanceDao = db.attendanceDao()

    @Provides
    @Singleton
    fun provideShiftDao(db: PresenceDatabase): ShiftDao = db.shiftDao()

    @Provides
    @Singleton
    fun provideShiftAssignmentDao(db: PresenceDatabase): ShiftAssignmentDao = db.shiftAssignmentDao()

    @Provides
    @Singleton
    fun provideOvertimeDao(db: PresenceDatabase): OvertimeDao = db.overtimeDao()

    @Provides
    @Singleton
    fun providePremisesDao(db: PresenceDatabase): PremisesDao = db.premisesDao()

    @Provides
    @Singleton
    fun provideEmployeeDao(db: PresenceDatabase): EmployeeDao = db.employeeDao()

    @Provides
    @Singleton
    fun provideLocationPingDao(db: PresenceDatabase): LocationPingDao = db.locationPingDao()

    @Provides
    @Singleton
    fun providePingSummaryDao(db: PresenceDatabase): PingSummaryDao = db.pingSummaryDao()

    @Provides
    @Singleton
    fun providePaymentRuleDao(db: PresenceDatabase): PaymentRuleDao = db.paymentRuleDao()

    @Provides
    @Singleton
    fun provideSalaryRecordDao(db: PresenceDatabase): SalaryRecordDao = db.salaryRecordDao()

    @Provides
    @Singleton
    fun provideSalaryTransactionDao(db: PresenceDatabase): SalaryTransactionDao = db.salaryTransactionDao()

    @Provides
    @Singleton
    fun provideHolidayDao(db: PresenceDatabase): HolidayDao = db.holidayDao()

    @Provides
    @Singleton
    fun provideLeaveRequestDao(db: PresenceDatabase): LeaveRequestDao = db.leaveRequestDao()

    @Provides
    @Singleton
    fun providePayrollSnapshotDao(db: PresenceDatabase): PayrollSnapshotDao = db.payrollSnapshotDao()

    @Provides
    @Singleton
    fun provideEmployeeHomeSnapshotDao(db: PresenceDatabase): EmployeeHomeSnapshotDao = db.employeeHomeSnapshotDao()

    @Provides
    @Singleton
    fun provideEmployeeAttendanceSnapshotDao(db: PresenceDatabase): EmployeeAttendanceSnapshotDao = db.employeeAttendanceSnapshotDao()

    @Provides
    @Singleton
    fun provideEmployeeSalarySnapshotDao(db: PresenceDatabase): EmployeeSalarySnapshotDao = db.employeeSalarySnapshotDao()

    @Provides
    @Singleton
    fun provideOwnerEmployeesSnapshotDao(db: PresenceDatabase): OwnerEmployeesSnapshotDao = db.ownerEmployeesSnapshotDao()

    @Provides
    @Singleton
    fun provideOwnerPingsSnapshotDao(db: PresenceDatabase): OwnerPingsSnapshotDao = db.ownerPingsSnapshotDao()

    @Provides
    @Singleton
    fun provideOwnerReportsSnapshotDao(db: PresenceDatabase): OwnerReportsSnapshotDao = db.ownerReportsSnapshotDao()

    @Provides
    @Singleton
    fun providePendingActionDao(db: PresenceDatabase): PendingActionDao = db.pendingActionDao()

    @Provides
    @Singleton
    fun provideSyncMetadataDao(db: PresenceDatabase): SyncMetadataDao = db.syncMetadataDao()

    @Provides
    @Singleton
    fun provideProcessedEventDao(db: PresenceDatabase): ProcessedEventDao = db.processedEventDao()
}
