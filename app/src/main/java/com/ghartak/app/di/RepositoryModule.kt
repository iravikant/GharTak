package com.ghartak.app.di

import com.ghartak.app.data.repository.AuthRepositoryImpl
import com.ghartak.app.data.repository.DeliveryRepositoryImpl
import com.ghartak.app.domain.repository.AuthRepository
import com.ghartak.app.domain.repository.DeliveryRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryRepository(impl: DeliveryRepositoryImpl): DeliveryRepository
}
