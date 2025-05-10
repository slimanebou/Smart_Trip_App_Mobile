package com.example.app.models

import org.osmdroid.util.GeoPoint
import java.time.LocalDate

data class Itinerary(
    var name: String? = null,         // Nom de l'itinéraire
    var ville_depart: String? = null, // La ville de départ
    var date_debut: String? = null,   // Date de lancement du trajet
    var date_fin: String? = null,     // Date de fin du trajet
    // Les points de l'itinéraires
    var it_points: MutableList<GeoPoint> = mutableListOf(),
    // Les points d'intérêts
    var interst_points: MutableList<PointOfInterest> = mutableListOf(),
    // Liste des photos reliées à l'itinéraire
    var it_photos: MutableList<PhotoModel> = mutableListOf(),
    // Public ou pas
    var isPublic : Boolean =  false,
    // Code du pays
    var countryCode : String? = null
) {

    fun ajouterPointInteret(poi: PointOfInterest) {
        // Méthode pour rajouter un point d'intérêt à la liste
        interst_points.add(poi)
    }
}
