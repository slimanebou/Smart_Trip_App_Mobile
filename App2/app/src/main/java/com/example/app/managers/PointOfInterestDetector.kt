package com.example.app.managers

import com.example.app.helpers.GeoHelper
import com.example.app.models.PointOfInterest
import com.example.app.models.Itinerary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class PointOfInterestDetector(
    private val itinerary: Itinerary,
    private val detectionRadiusMeters: Double = 500.0,  // Rayon de détection
    private val stayDurationMillis: Long = 10 * 60 * 1000  // Temps d'arrêt minimum
) {
    private var lastLocation: GeoPoint? = null
    private var stationaryStartTime: Long = 0L

    fun processLocationUpdate(newLocation: GeoPoint) {
        val now = System.currentTimeMillis()

        if (lastLocation == null) {
            lastLocation = newLocation
            stationaryStartTime = now
            return
        }

        val distance = lastLocation!!.distanceToAsDouble(newLocation)

        if (distance < detectionRadiusMeters) {
            // Si l'utilisateur reste proche du même point
            if (now - stationaryStartTime >= stayDurationMillis) {
                // ➔ Nouveau point d'intérêt détecté
                detectPoiNameAndAdd(newLocation)
                stationaryStartTime = now // Reset après détection
            }
        } else {
            // L'utilisateur a bougé
            lastLocation = newLocation
            stationaryStartTime = now
        }
    }

    private fun detectPoiNameAndAdd(location: GeoPoint) {
        // Lance une coroutine pour appeler l'API
        CoroutineScope(Dispatchers.IO).launch {
            val placeName = GeoHelper.getPlaceName(location.latitude, location.longitude)

            //  Ici, remplace ton User-Agent manuellement :
            val name = placeName ?: "Point d'intérêt inconnu"

            itinerary.ajouterPointInteret(PointOfInterest(name, location = location))

        }
    }
}
