package com.example.app.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.app.R
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.example.app.models.Itinerary

object MapManager {

    private var currentPositionMarker: Marker? = null


    /*fun markPosition(mapView: MapView, position: GeoPoint, message: String = "Vous êtes ici") {
        val marker = Marker(mapView)
        marker.position = position
        marker.title = message
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
        mapView.controller.setCenter(position)
        mapView.invalidate()
    }*/

    fun setToNull() {
        currentPositionMarker = null
    }


    fun markPosition(mapView: MapView, position: GeoPoint, message: String = "Vous êtes ici", center : Boolean) {
        if (currentPositionMarker == null) {
            currentPositionMarker = Marker(mapView).apply {
                title = message
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(this)
            }
        }
        if (center) {mapView.controller.setCenter(position)}
        currentPositionMarker?.position = position
        mapView.invalidate()
    }


    fun drawItinerary(itinerary: Itinerary, mapView: MapView) {
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
            icon = resizeIcon(mapView.context, R.drawable.ic_start_marker, 48, 48)
        }
        mapView.overlays.add(startMarker)

        // 3. Marquer l'arrivée
        val endMarker = Marker(mapView).apply {
            position = itinerary.it_points.last()
            title = "Arrivée"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = resizeIcon(mapView.context, R.drawable.ic_end_marker, 48, 48)
        }
        mapView.overlays.add(endMarker)

        // 4. Ajouter les POIs
        for (poi in itinerary.interst_points) {
            val poiMarker = Marker(mapView).apply {
                position = poi.location
                title = poi.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = resizeIcon(mapView.context, R.drawable.ic_poi_marker, 48, 48)
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
                    icon = resizeIcon(mapView.context, R.drawable.ic_photo_marker, 48, 48)
                    setOnMarkerClickListener { _, _ ->
                        showPhotoDialog(mapView.context, photo.uri, photo.urlFirebase)
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


    private fun showPhotoDialog(context: Context, photoUri: Uri?, url: String?) {
        val imageView = ImageView(context).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val dialog = AlertDialog.Builder(context)
            .setView(imageView)
            .setPositiveButton("Fermer") { d, _ -> d.dismiss() }
            .create()

        if (photoUri != null && photoUri != Uri.EMPTY) {
            imageView.setImageURI(photoUri)
            dialog.show()
        } else if (!url.isNullOrEmpty()) {
            Thread {
                try {
                    val input = java.net.URL(url).openStream()
                    val bitmap = BitmapFactory.decodeStream(input)
                    (context as? android.app.Activity)?.runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                        dialog.show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        } else {
            // fallback
            imageView.setImageResource(R.drawable.ic_photo_marker)
            dialog.show()
        }
    }


    private fun resizeIcon(context: Context, @DrawableRes resId: Int, widthDp: Int, heightDp: Int): BitmapDrawable? {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        val metrics = context.resources.displayMetrics
        val widthPx = (widthDp * metrics.density).toInt()
        val heightPx = (heightDp * metrics.density).toInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDrawable(context.resources, bitmap)
    }




}