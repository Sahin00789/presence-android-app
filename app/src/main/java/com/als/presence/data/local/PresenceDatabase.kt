package com.als.presence.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main Room Database for the Presence Android Client.
 *
 * Persists local cache for Users, Organizations, Premises, Employees, Shifts, Attendance,
 * Location Pings, Payroll Snapshots, and Offline Sync Metadata.
 */
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
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PresenceDatabase : RoomDatabase() {
    /** Data Access Object for User entities. */
    abstract fun userDao(): UserDao

    /** Data Access Object for Owner entities. */
    abstract fun ownerDao(): OwnerDao

    /** Data Access Object for Organization entities. */
    abstract fun organizationDao(): OrganizationDao

    /** Data Access Object for Premises entities. */
    abstract fun premisesDao(): PremisesDao

    /** Data Access Object for Employee entities. */
    abstract fun employeeDao(): EmployeeDao

    /** Data Access Object for Shift templates. */
    abstract fun shiftDao(): ShiftDao

    /** Data Access Object for Shift roster assignments. */
    abstract fun shiftAssignmentDao(): ShiftAssignmentDao

    /** Data Access Object for Overtime records. */
    abstract fun overtimeDao(): OvertimeDao

    /** Data Access Object for Attendance clock-in entries. */
    abstract fun attendanceDao(): AttendanceDao

    /** Data Access Object for GPS location pings. */
    abstract fun locationPingDao(): LocationPingDao

    /** Data Access Object for daily ping summary counts. */
    abstract fun pingSummaryDao(): PingSummaryDao

    /** Data Access Object for unworked payment rules. */
    abstract fun paymentRuleDao(): PaymentRuleDao

    /** Data Access Object for salary ledger records. */
    abstract fun salaryRecordDao(): SalaryRecordDao

    /** Data Access Object for payroll disbursement transactions. */
    abstract fun salaryTransactionDao(): SalaryTransactionDao

    /** Data Access Object for organization holidays. */
    abstract fun holidayDao(): HolidayDao

    /** Data Access Object for staff leave requests. */
    abstract fun leaveRequestDao(): LeaveRequestDao

    /** Data Access Object for system notifications. */
    abstract fun notificationDao(): NotificationDao

    /** Data Access Object for individual location checks. */
    abstract fun locationCheckDao(): LocationCheckDao

    /** Data Access Object for payroll snapshot projections. */
    abstract fun payrollSnapshotDao(): PayrollSnapshotDao

    /** Data Access Object for employee home screen UI snapshots. */
    abstract fun employeeHomeSnapshotDao(): EmployeeHomeSnapshotDao

    /** Data Access Object for employee attendance summary snapshots. */
    abstract fun employeeAttendanceSnapshotDao(): EmployeeAttendanceSnapshotDao

    /** Data Access Object for employee salary summary snapshots. */
    abstract fun employeeSalarySnapshotDao(): EmployeeSalarySnapshotDao

    /** Data Access Object for owner employees list snapshots. */
    abstract fun ownerEmployeesSnapshotDao(): OwnerEmployeesSnapshotDao

    /** Data Access Object for owner pings/live monitor snapshots. */
    abstract fun ownerPingsSnapshotDao(): OwnerPingsSnapshotDao

    /** Data Access Object for owner reports snapshots. */
    abstract fun ownerReportsSnapshotDao(): OwnerReportsSnapshotDao

    /** Data Access Object for pending offline actions queue. */
    abstract fun pendingActionDao(): PendingActionDao

    /** Data Access Object for sync domain metadata. */
    abstract fun syncMetadataDao(): SyncMetadataDao

    /** Data Access Object for processed WebSocket deduplication events. */
    abstract fun processedEventDao(): ProcessedEventDao
}
