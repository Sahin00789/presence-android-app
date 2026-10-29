package com.als.presence.di

import com.als.presence.data.repository.*
import com.als.presence.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOvertimeRepository(
        overtimeRepositoryImpl: OvertimeRepositoryImpl
    ): OvertimeRepository

    @Binds
    @Singleton
    abstract fun bindEmployeeRepository(
        employeeRepositoryImpl: EmployeeRepositoryImpl
    ): EmployeeRepository

    @Binds
    @Singleton
    abstract fun bindPremisesRepository(
        premisesRepositoryImpl: PremisesRepositoryImpl
    ): PremisesRepository

    @Binds
    @Singleton
    abstract fun bindPayrollRepository(
        payrollRepositoryImpl: PayrollRepositoryImpl
    ): PayrollRepository

    @Binds
    @Singleton
    abstract fun bindLeaveRepository(
        leaveRepositoryImpl: LeaveRepositoryImpl
    ): LeaveRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(
        attendanceRepositoryImpl: AttendanceRepositoryImpl
    ): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindOperationsRepository(
        operationsRepositoryImpl: OperationsRepositoryImpl
    ): OperationsRepository
}
