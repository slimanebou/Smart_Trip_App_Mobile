package com.example.app.models

import org.osmdroid.util.GeoPoint

data class PointOfInterest(
    val location: GeoPoint,
    val name: String
)
