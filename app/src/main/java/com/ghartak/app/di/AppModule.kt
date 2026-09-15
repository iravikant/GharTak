package com.ghartak.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Reserved for app-wide providers that don't fit NetworkModule/DatabaseModule
 * (e.g. Dispatchers, Firebase instances). SessionManager uses an @Inject constructor
 * directly (see SessionManager.kt) so it does not need a @Provides method here.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
