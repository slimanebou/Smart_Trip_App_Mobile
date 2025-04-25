package com.example.app.models

import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import android.net.Uri

import java.io.Serializable


data class Voyage(
    var id: String = "",
    var nom: String = "",
    var description: String = "",
    var villeDepart: String = "",
    var dateDebut: String = "",
    var dateFin: String = "",
    var isPublic: Boolean = false,
    var points: List<FirestoreGeoPoint> = emptyList(),
    var photos: List<PhotoMeta> = emptyList(),
    var coverPhotoUrl: String? = null,
    var utilisateur: String = "",
    var ownerFirstName: String? = null,
    var ownerLastName: String? = null,
    var ownerPhotoUrl: String? = null,
    var isTripPublic : Boolean = false,
    var countryCode : String? = null
) : Serializable

fun Voyage.toItinerary(): Itinerary {
    return Itinerary(
        name = nom,
        ville_depart = villeDepart,
        date_debut = dateDebut, // Déjà en "yyyy-MM-dd"
        date_fin = dateFin,
        it_points = points.map { OsmGeoPoint(it.latitude, it.longitude) }.toMutableList(),
        it_photos = photos.map {
            PhotoModel(
                uri = Uri.EMPTY,
                position = OsmGeoPoint(it.latitude, it.longitude),
                date = it.date,
                attachedPoiName = it.commentaire,
                urlFirebase = it.url
            )
        }.toMutableList()
    )
}
