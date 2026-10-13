package com.ghartak.app.di

import com.ghartak.app.data.repository.AuthRepositoryImpl
import com.ghartak.app.data.repository.DeliveryRepositoryImpl
import com.ghartak.app.data.repository.ProfileRepositoryImpl
import com.ghartak.app.data.repository.RatingRepositoryImpl
import com.ghartak.app.data.repository.RiderJobRepositoryImpl
import com.ghartak.app.data.repository.RiderRepositoryImpl
import com.ghartak.app.data.repository.SavedAddressRepositoryImpl
import com.ghartak.app.data.repository.SupportRepositoryImpl
import com.ghartak.app.domain.repository.AuthRepository
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.domain.repository.ProfileRepository
import com.ghartak.app.domain.repository.RatingRepository
import com.ghartak.app.domain.repository.RiderJobRepository
import com.ghartak.app.domain.repository.RiderRepository
import com.ghartak.app.domain.repository.SavedAddressRepository
import com.ghartak.app.domain.repository.SupportRepository
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

    @Binds
    @Singleton
    abstract fun bindSavedAddressRepository(impl: SavedAddressRepositoryImpl): SavedAddressRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindRiderRepository(impl: RiderRepositoryImpl): RiderRepository

    @Binds
    @Singleton
    abstract fun bindRiderJobRepository(impl: RiderJobRepositoryImpl): RiderJobRepository

    @Binds
    @Singleton
    abstract fun bindSupportRepository(impl: SupportRepositoryImpl): SupportRepository

    @Binds
    @Singleton
    abstract fun bindRatingRepository(impl: RatingRepositoryImpl): RatingRepository
}
