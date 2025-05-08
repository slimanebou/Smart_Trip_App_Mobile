package com.example.app.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

object MapHelper {
    @SuppressLint("MissingPermission")
    fun centerOnUserPosition(context: Context, mapView: MapView) {
        if (PermissionHelper.hasLocationPermission(context)) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    mapView.controller.setCenter(point)
                    mapView.controller.setZoom(18.0)
                } else {
                    Toast.makeText(context, "Position non disponible", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permission manquante", Toast.LENGTH_SHORT).show()
        }
    }
}
