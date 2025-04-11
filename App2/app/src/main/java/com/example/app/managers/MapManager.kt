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

        val polyline = Polyline()
        polyline.setPoints(itinerary!!.it_points)

        polyline.width = 5f
        polyline.color = Color.BLUE

        mapView.overlays.add(polyline)
        mapView.invalidate()

        val boundingBox = BoundingBox.fromGeoPoints(itinerary.it_points)
        mapView.zoomToBoundingBox(boundingBox, true, 50)
    }
}