package com.example.app.models

import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import android.net.Uri
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Voyage(
    val id: String = "",
    val nom: String = "",
    val description: String = "",
    val villeDepart: String = "",
    val dateDebut: Long = 0L, // Epoch day
    val dateFin: Long = 0L,   // Epoch day
    val points: List<FirestoreGeoPoint> = emptyList(),
    val photos: List<PhotoMeta> = emptyList()
) : Serializable

fun Voyage.toItinerary(): Itinerary {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val dateDebutFormatted = formatter.format(Date(dateDebut * 86400000)) // 86400000ms = 1 day
    val dateFinFormatted = formatter.format(Date(dateFin * 86400000))

    return Itinerary(
        name = nom,
        ville_depart = villeDepart,
        date_debut = dateDebutFormatted,
        date_fin = dateFinFormatted,
        it_points = points.map { OsmGeoPoint(it.latitude, it.longitude) }.toMutableList(),
        it_photos = photos.map {
            PhotoModel(
                uri = Uri.EMPTY, // pas utilisé ici
                position = OsmGeoPoint(it.latitude, it.longitude),
                attachedPoiName = it.commentaire,
                urlFirebase = it.url
            )
        }.toMutableList()
    )
}
