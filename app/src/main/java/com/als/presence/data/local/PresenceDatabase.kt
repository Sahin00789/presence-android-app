package com.als.presence.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        OwnerEntity::class,
        OrganizationEntity::class,
        PremisesEntity::class,
        EmployeeEntity::class,
        ShiftEntity::class,
        ShiftAssignmentEntity::class,
        OvertimeEntity::class,
        AttendanceEntity::class,
        LocationPingEntity::class,
        PingSummaryEntity::class,
        PaymentRuleEntity::class,
        SalaryRecordEntity::class,
        SalaryTransactionEntity::class,
        HolidayEntity::class,
        LeaveRequestEntity::class,
        NotificationEntity::class,
        LocationCheckEntity::class,
        PayrollSnapshotEntity::class,
        MonthlyEarningSnapshotEntity::class,
        EmployeeHomeSnapshotEntity::class,
        EmployeeAttendanceSnapshotEntity::class,
        EmployeeSalarySnapshotEntity::class,
        OwnerEmployeesSnapshotEntity::class,
        OwnerPingsSnapshotEntity::class,
        OwnerReportsSnapshotEntity::class,
        PendingActionEntity::class,
        SyncMetadataEntity::class,
        ProcessedEventEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PresenceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun ownerDao(): OwnerDao
    abstract fun organizationDao(): OrganizationDao
    abstract fun premisesDao(): PremisesDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun shiftDao(): ShiftDao
    abstract fun shiftAssignmentDao(): ShiftAssignmentDao
    abstract fun overtimeDao(): OvertimeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun locationPingDao(): LocationPingDao
    abstract fun pingSummaryDao(): PingSummaryDao
    abstract fun paymentRuleDao(): PaymentRuleDao
    abstract fun salaryRecordDao(): SalaryRecordDao
    abstract fun salaryTransactionDao(): SalaryTransactionDao
    abstract fun holidayDao(): HolidayDao
    abstract fun leaveRequestDao(): LeaveRequestDao
    abstract fun notificationDao(): NotificationDao
    abstract fun locationCheckDao(): LocationCheckDao
    abstract fun payrollSnapshotDao(): PayrollSnapshotDao
    abstract fun employeeHomeSnapshotDao(): EmployeeHomeSnapshotDao
    abstract fun employeeAttendanceSnapshotDao(): EmployeeAttendanceSnapshotDao
    abstract fun employeeSalarySnapshotDao(): EmployeeSalarySnapshotDao
    abstract fun ownerEmployeesSnapshotDao(): OwnerEmployeesSnapshotDao
    abstract fun ownerPingsSnapshotDao(): OwnerPingsSnapshotDao
    abstract fun ownerReportsSnapshotDao(): OwnerReportsSnapshotDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun processedEventDao(): ProcessedEventDao
}
