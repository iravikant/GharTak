package com.ghartak.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotating with @HiltAndroidApp triggers Hilt's code generation,
 * including a base class for the Application that serves as the app-level dependency container.
 */
@HiltAndroidApp
class GharTakApp : Application()
