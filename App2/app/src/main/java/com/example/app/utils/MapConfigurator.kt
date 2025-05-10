package com.example.app.utils

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView

object MapConfigurator {

    fun initializeOSMDroid(context: Context) {
        // Configuration de la bibliothèque OSM
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    fun configureMap(mapView: MapView) {
        // Les tuiles
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        // Activation du Zoom
        mapView.setMultiTouchControls(true)
        // Zoom par défaut
        mapView.controller.setZoom(15.0)
    }
}
