package com.example.app.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.app.R
import com.example.app.models.PhotoModel
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

    fun drawItinerary(itinerary: itinerary, mapView: MapView) {
        if (itinerary.it_points.size < 2) return // pas assez de points pour dessiner

        mapView.overlays.clear()  // Supprime tous les anciens marqueurs (y compris "Vous êtes ici")

        // 1. Trace la ligne de l'itinéraire
        val polyline = Polyline()
        polyline.setPoints(itinerary.it_points)
        mapView.overlays.add(polyline)

        // 2. Marquer le départ
        val startMarker = Marker(mapView).apply {
            position = itinerary.it_points.first()
            title = "Départ"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            val drawable = ContextCompat.getDrawable(mapView.context, R.drawable.ic_start_marker)
            icon = drawable?.let { resizeDrawable(it, 48, 48) }
        }
        mapView.overlays.add(startMarker)

        // 3. Marquer l'arrivée
        val endMarker = Marker(mapView).apply {
            position = itinerary.it_points.last()
            title = "Arrivée"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            val drawable = ContextCompat.getDrawable(mapView.context, R.drawable.ic_end_marker)
            icon = drawable?.let { resizeDrawable(it, 48, 48) }
        }
        mapView.overlays.add(endMarker)

        // 4. Ajouter les POIs
        for (poi in itinerary.interst_points) {
            val poiMarker = Marker(mapView).apply {
                position = poi.location
                title = poi.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                val drawable = ContextCompat.getDrawable(mapView.context, R.drawable.ic_poi_marker)
                icon = drawable?.let { resizeDrawable(it, 48, 48) }
            }
            mapView.overlays.add(poiMarker)
        }

        // 5. Ajouter les photos
        for (photo in itinerary.it_photos) {
            if (photo.position != null) {
                val photoMarker = Marker(mapView).apply {
                    position = photo.position
                    title = "Photo"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    val drawable = ContextCompat.getDrawable(mapView.context, R.drawable.ic_photo_marker)
                    icon = drawable?.let { resizeDrawable(it, 48, 48) }
                    setOnMarkerClickListener { marker, _ ->
                        photo.uri?.let { uri ->
                            showPhotoDialog(mapView.context, uri)
                        }
                        true
                    }
                }
                mapView.overlays.add(photoMarker)
            }
        }



        // 5. Adapter la vue
        val boundingBox = BoundingBox.fromGeoPoints(itinerary.it_points)
        mapView.zoomToBoundingBox(boundingBox, true, 50)

        // 6. Refresh de la map
        mapView.invalidate()
    }


    private fun showPhotoDialog(context: Context, photoUri: Uri) {
        val imageView = ImageView(context).apply {
            setImageURI(photoUri)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        AlertDialog.Builder(context)
            .setView(imageView)
            .setPositiveButton("Fermer") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun resizeDrawable(drawable: Drawable, width: Int, height: Int): Drawable {
        val bitmap = (drawable as BitmapDrawable).bitmap
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, false)
        return BitmapDrawable(resized)
    }



}