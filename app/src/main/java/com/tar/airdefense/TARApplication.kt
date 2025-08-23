package com.tar.airdefense

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.tar.airdefense.data.database.TARDatabase
import com.tar.airdefense.data.network.NetworkModule
import com.tar.airdefense.worker.AirSurveillanceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * TAR (تار) - Air Defense Surveillance System
 * Main application class for the comprehensive air defense and drone detection system
 * 
 * Features:
 * - Real-time air traffic monitoring
 * - Drone threat detection and classification
 * - Multi-language support (Persian, English, Russian, Swedish)
 * - C4ISR integration
 * - HTTP/3 support for high-speed data transmission
 * - Integration with multiple flight data sources
 */
class TARApplication : Application(), Configuration.Provider {
    
    companion object {
        const val CHANNEL_ID_AIR_ALERT = "air_alert_channel"
        const val CHANNEL_ID_SURVEILLANCE = "surveillance_channel"
        const val CHANNEL_ID_COMMAND = "command_channel"
        
        lateinit var instance: TARApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Create notification channels
        createNotificationChannels()
        
        // Initialize database
        initializeDatabase()
        
        // Initialize network components
        initializeNetwork()
        
        // Start air surveillance worker
        startAirSurveillance()
        
        Timber.i("TAR Air Defense System initialized successfully")
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val airAlertChannel = NotificationChannel(
                CHANNEL_ID_AIR_ALERT,
                "Air Alert Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical air defense alerts and threat notifications"
                enableVibration(true)
                enableLights(true)
            }
            
            val surveillanceChannel = NotificationChannel(
                CHANNEL_ID_SURVEILLANCE,
                "Surveillance Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Regular air surveillance updates and status reports"
            }
            
            val commandChannel = NotificationChannel(
                CHANNEL_ID_COMMAND,
                "Command Center",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct communications from air force command center"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(
                listOf(airAlertChannel, surveillanceChannel, commandChannel)
            )
        }
    }
    
    private fun initializeDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TARDatabase.getInstance(this@TARApplication)
                Timber.i("Database initialized successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize database")
            }
        }
    }
    
    private fun initializeNetwork() {
        try {
            NetworkModule.initialize(this)
            Timber.i("Network components initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize network components")
        }
    }
    
    private fun startAirSurveillance() {
        try {
            val workManager = WorkManager.getInstance(this)
            AirSurveillanceWorker.startPeriodicWork(workManager)
            Timber.i("Air surveillance worker started successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start air surveillance worker")
        }
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .setDefaultProcessName("com.tar.airdefense")
            .build()
    }
}