package com.example.app.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

object MapSearchHelper {
    @SuppressLint("MissingPermission")
    fun setupSearchBar(
        searchEditText: EditText,
        mapView: MapView,
        lifecycleScope: CoroutineScope,
        context: Context
    ) {
        searchEditText.doOnTextChanged { text, _, _, _ ->
            val query = text.toString().trim()

            if (query.isEmpty()) {
                // Recentrer sur la position actuelle
                if (query.isEmpty()) {
                    MapHelper.centerOnUserPosition(context, mapView)
                    return@doOnTextChanged
                }
            }

            if (query.length >= 3) {
                lifecycleScope.launch {
                    val result = GeoHelper.searchCityCoordinates(context, query)
                    if (result != null) {
                        val (lat, lon) = result
                        val point = GeoPoint(lat, lon)
                        mapView.controller.setCenter(point)
                        mapView.controller.setZoom(14.5)
                    } else {
                        Toast.makeText(context, "Ville introuvable", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
