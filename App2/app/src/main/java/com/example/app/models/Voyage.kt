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
    val dateDebut: String = "",
    val dateFin: String = "",
    val points: List<FirestoreGeoPoint> = emptyList(),
    val photos: List<PhotoMeta> = emptyList()
) : Serializable

fun Voyage.toItinerary(): Itinerary {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val dateDebutFormatted = try {
        formatter.format(formatter.parse(dateDebut) ?: Date())
    } catch (e: Exception) {
        "?"
    }

    val dateFinFormatted = try {
        formatter.format(formatter.parse(dateFin) ?: Date())
    } catch (e: Exception) {
        "?"
    }

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

