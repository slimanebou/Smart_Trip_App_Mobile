package com.example.app.models

import org.osmdroid.util.GeoPoint

data class PointOfInterest(
    val name: String,
    val description: String = "",
    val location: GeoPoint
)
