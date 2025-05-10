package com.example.app.models

import org.osmdroid.util.GeoPoint

data class PointOfInterest(
    val name: String,               // Nom ou l'adresse du POI
    val description: String = "",   // Description d'un POI manuel
    val location: GeoPoint          // Sa localisation
)
