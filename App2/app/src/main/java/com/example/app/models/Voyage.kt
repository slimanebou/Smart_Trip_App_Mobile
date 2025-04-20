package com.example.app.models

import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import android.net.Uri
import java.io.Serializable

data class Voyage(
    val id: String = "",
    val nom: String = "",
    val description: String = "",
    val villeDepart: String = "",
    val dateDebut: String = "",
    val dateFin: String = "",
    val points: List<FirestoreGeoPoint> = emptyList(),
    val photos: List<PhotoMeta> = emptyList(),
    val coverPhotoUrl: String? = null
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
