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
        // Dés que y a un changement de texte
        searchEditText.doOnTextChanged { text, _, _, _ ->
            // On récupère le texte
            val query = text.toString().trim()
            // S'il est vide on ce recentre sur la position actuelle
            if (query.isEmpty()) {
                MapHelper.centerOnUserPosition(context, mapView)
                return@doOnTextChanged
            }

            // Si le texte est de longueur sup à 3
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
