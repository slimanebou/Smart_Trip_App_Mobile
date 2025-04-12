package com.example.app.managers

import android.graphics.Color
import android.widget.Toast
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.example.app.models.itinerary

object MapManager {

    fun markPosition(mapView: MapView, position: GeoPoint, message: String = "Vous êtes ici") {
        val marker = Marker(mapView)
        marker.position = position
        marker.title = message
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
        mapView.controller.setCenter(position)
        mapView.invalidate()
    }

    fun drawItinerary(mapView: MapView, itinerary: itinerary?) {
        if (itinerary?.it_points.isNullOrEmpty()) {
            Toast.makeText(mapView.context, "Aucun trajet à afficher", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Dessiner la polyline
        val polyline = Polyline()
        polyline.setPoints(itinerary!!.it_points)
        polyline.width = 5f
        polyline.color = Color.BLUE
        mapView.overlays.add(polyline)

        // 2. Ajouter un marker de départ
        val startMarker = Marker(mapView)
        startMarker.position = itinerary.it_points.first()
        startMarker.title = "Départ"
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(startMarker)

        // 3. Ajouter un marker d'arrivée
        val endMarker = Marker(mapView)
        endMarker.position = itinerary.it_points.last()
        endMarker.title = "Arrivée"
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(endMarker)

        // 4. Ajouter des markers pour les Points d'Intérêt (POIs)
        itinerary.interst_points?.forEach { poi ->
            val poiMarker = Marker(mapView)
            poiMarker.position = poi.location
            poiMarker.title = poi.name ?: "Point d'intérêt"
            poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(poiMarker)
        }

        // 5. Adapter la vue
        val boundingBox = BoundingBox.fromGeoPoints(itinerary.it_points)
        mapView.zoomToBoundingBox(boundingBox, true, 50)

        // 6. Refresh de la map
        mapView.invalidate()
    }

}