package com.example.app.models

import org.osmdroid.util.GeoPoint
import java.time.LocalDate

data class Itinerary(
    var name: String? = null,
    var ville_depart: String? = null,
    var date_debut: String? = null,
    var date_fin: String? = null,
    var it_points: MutableList<GeoPoint> = mutableListOf(),
    var interst_points: MutableList<PointOfInterest> = mutableListOf(),
    var it_photos: MutableList<PhotoModel> = mutableListOf(),
    var isPublic : Boolean =  false
) {
    fun ajouterPointInteret(poi: PointOfInterest) {
        interst_points.add(poi)
    }

    fun ajouterPointItineraire(point: GeoPoint) {
        it_points.add(point)
    }
}
