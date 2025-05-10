
package com.example.app.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.app.R
import com.example.app.helpers.PermissionHelper
import com.example.app.managers.JourneyManager
import com.example.app.managers.PointOfInterestDetector
import com.example.app.receivers.BatteryReceiver
import com.google.android.gms.location.*
import org.osmdroid.util.GeoPoint

class GpsTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var poiDetector: PointOfInterestDetector? = null
    private var batteryReceiver: BatteryReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("SERVICE", "GpsTrackingService lancé")
        instance = this

        batteryReceiver = BatteryReceiver {
            Log.d("BATTERY", "Niveau critique détecté → Pause du voyage")
            pauseTrackingGps()
        }

        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, intentFilter)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // L'interval de récupération de coordonnées GPS est de 5 minutes
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 300000L)
            .setMinUpdateIntervalMillis(300000L)
            .build()

        // Initialisation du détécteur de POIs
        JourneyManager.currentItinerary?.let {
            poiDetector = PointOfInterestDetector(it)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // à chaque capture
                super.onLocationResult(locationResult)
                if (paused) return

                for (location in locationResult.locations) {
                    val newPoint = GeoPoint(location.latitude, location.longitude)
                    JourneyManager.currentItinerary?.it_points?.add(newPoint)
                    poiDetector?.processLocationUpdate(newPoint)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("SERVICE", "GpsTrackingService arrêté")
        batteryReceiver?.let { unregisterReceiver(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Suivi en cours")
            .setContentText("L'application suit votre trajet")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "gps_channel"
        const val CHANNEL_NAME = "Suivi GPS"
        var paused: Boolean = false
        var instance: GpsTrackingService? = null
    }

    fun stopTracking() {
        // Méthode pour arrêter le service complètement
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun pauseTrackingGps() {
        // Méthode pour mettre le service en pause
        if (!paused) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            paused = true
            Log.d("GPS", "Tracking mis en pause")
        }
    }

    @SuppressLint("MissingPermission")
    fun resumeTrackingGps() {
        // Méthode pour reprende le service
        if (paused && PermissionHelper.hasLocationPermission(this)) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            paused = false
            Log.d("GPS", "Tracking repris")
        }
    }
}
