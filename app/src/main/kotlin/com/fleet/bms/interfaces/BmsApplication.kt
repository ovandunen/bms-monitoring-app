package com.fleet.bms.interfaces

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application Class
 * 
 * Initialize app-wide dependencies and logging.
 */
@HiltAndroidApp
class BmsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.i("BMS Application started")
    }
}
