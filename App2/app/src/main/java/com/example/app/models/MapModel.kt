package com.example.app.models

import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapModel {

    private val markers: MutableList<Marker> = mutableListOf()
    private var routePolyline: Polyline? = null

    fun addMarker(marker: Marker) {
        markers.add(marker)
    }

    fun setRoute(polyline: Polyline) {
        routePolyline = polyline
    }

    fun clear() {
        markers.clear()
        routePolyline = null
    }


}
