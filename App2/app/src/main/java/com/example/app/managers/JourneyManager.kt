package com.example.app.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.app.models.*
import com.example.app.service.GpsTrackingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

object JourneyManager {

    var currentItinerary: Itinerary? = null

    fun startJourney(context: Context, ville: String?, date: java.time.LocalDate, name: String?) {
        Toast.makeText(context, "Voyage démarré !", Toast.LENGTH_SHORT).show()

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatted = formatter.format(date)

        currentItinerary = Itinerary(
            name = name,
            ville_depart = ville,
            date_debut = dateFormatted
        )

        context.startService(Intent(context, GpsTrackingService::class.java))
    }

    fun stopJourney(context: Context) {
        context.stopService(Intent(context, GpsTrackingService::class.java))

        val itinerary = currentItinerary ?: return
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayFormatted = formatter.format(Date())
        itinerary.date_fin = todayFormatted

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val voyageId = UUID.randomUUID().toString()
        val firestorePoints = itinerary.it_points.map {
            com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
        }

        val uploadedPhotos = mutableListOf<PhotoMeta>()
        val totalPhotos = itinerary.it_photos.size

        if (totalPhotos == 0) {
            saveVoyageToFirestore(userId, voyageId, itinerary, firestorePoints, uploadedPhotos, context)
            return
        }

        var uploadCount = 0

        itinerary.it_photos.forEach { photo ->
            val uri = photo.uri ?: return@forEach
            val filename = "${UUID.randomUUID()}.jpg"
            val ref = FirebaseStorage.getInstance().getReference("voyages/$userId/$filename")

            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Erreur upload")
                ref.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                val meta = PhotoMeta(
                    url = downloadUri.toString(),
                    date = System.currentTimeMillis(),
                    latitude = photo.position?.latitude ?: 0.0,
                    longitude = photo.position?.longitude ?: 0.0,
                    commentaire = photo.attachedPoiName ?: "",
                    utilisateur = userId
                )
                uploadedPhotos.add(meta)
                uploadCount++
                if (uploadCount == totalPhotos) {
                    saveVoyageToFirestore(userId, voyageId, itinerary, firestorePoints, uploadedPhotos, context)
                }
            }.addOnFailureListener {
                uploadCount++
                if (uploadCount == totalPhotos) {
                    saveVoyageToFirestore(userId, voyageId, itinerary, firestorePoints, uploadedPhotos, context)
                }
            }
        }
    }

    private fun saveVoyageToFirestore(
        userId: String,
        voyageId: String,
        itinerary: Itinerary,
        points: List<com.google.firebase.firestore.GeoPoint>,
        photos: List<PhotoMeta>,
        context: Context
    ) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val dateDebutLong = itinerary.date_debut?.let {
            formatter.parse(it)?.time?.div(86400000)
        } ?: 0L

        val dateFinLong = itinerary.date_fin?.let {
            formatter.parse(it)?.time?.div(86400000)
        } ?: 0L

        val voyage = Voyage(
            id = voyageId,
            nom = itinerary.name ?: "Voyage sans nom",
            description = "",
            villeDepart = itinerary.ville_depart ?: "",
            dateDebut = dateDebutLong,
            dateFin = dateFinLong,
            points = points,
            photos = photos
        )

        FirebaseFirestore.getInstance()
            .collection("Utilisateurs").document(userId)
            .collection("voyages").document(voyageId)
            .set(voyage)
            .addOnSuccessListener {
                Toast.makeText(context, "Voyage enregistré avec succès", Toast.LENGTH_SHORT).show()
                Log.d("Firebase", "Voyage enregistré avec succès")
                currentItinerary = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "Erreur: ${e.message}")
            }
    }
}
