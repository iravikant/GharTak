package com.ghartak.app.di

import android.content.Context
import androidx.room.Room
import com.ghartak.app.data.local.AppDatabase
import com.ghartak.app.data.local.dao.DeliveryDao
import com.ghartak.app.data.local.dao.SavedAddressDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDeliveryDao(db: AppDatabase): DeliveryDao = db.deliveryDao()

    @Provides
    fun provideSavedAddressDao(db: AppDatabase): SavedAddressDao = db.savedAddressDao()
}
