package com.als.presence.di

import com.als.presence.data.repository.AuthRepositoryImpl
import com.als.presence.data.repository.OperationsRepositoryImpl
import com.als.presence.domain.repository.AuthRepository
import com.als.presence.domain.repository.OperationsRepository
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
    abstract fun bindOperationsRepository(
        operationsRepositoryImpl: OperationsRepositoryImpl
    ): OperationsRepository
}
