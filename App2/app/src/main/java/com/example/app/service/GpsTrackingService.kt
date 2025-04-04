package com.example.app.service

import com.example.app.R
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit

class GpsTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        Log.d("SERVICE", "GpsTrackingService lancé")

        // Initialisation du client GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Définition de la fréquence : toutes les 15 minutes
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.MINUTES.toMillis(15)
        ).build()

        // Callback de localisation
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("SERVICE", "Coordonnées GPS : ${location.latitude}, ${location.longitude}")
                    // Ici tu peux stocker les points dans une base locale ou un fichier
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        // Lancer la mise à jour GPS
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
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("SERVICE", "GpsTrackingService arrêté")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildNotification(): Notification {
        val channelId = "gps_channel"
        val channelName = "Suivi GPS"

        // Créer le channel (obligatoire depuis Android 8)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Suivi en cours")
            .setContentText("L'application suit votre trajet")
            .setSmallIcon(R.drawable.ic_location) // mets une icône qui existe dans ton projet
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
