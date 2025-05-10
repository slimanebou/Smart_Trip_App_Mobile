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
        // Si on possède les permissions de localisation
        if (PermissionHelper.hasLocationPermission(context)) {
            // On récupère le client de localisation Google
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            // On essaye de récupèrer la dernière localisation connue
            fusedClient.lastLocation.addOnSuccessListener { location ->
                // Si elle est différente de null
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    // On centralise le Point
                    mapView.controller.setCenter(point)
                    // On zoom dessus
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
