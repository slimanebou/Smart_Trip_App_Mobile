package com.example.app.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.app.models.*
import com.example.app.service.GpsTrackingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*
import androidx.exifinterface.media.ExifInterface

object JourneyManager {

    var currentItinerary: Itinerary? = null

    fun startJourney(context: Context, ville: String?, date: java.time.LocalDate, name: String?,
                     isPublic : Boolean, countryCode : String) {
        Toast.makeText(context, "Voyage démarré !", Toast.LENGTH_SHORT).show()

        val dateFormatted = date.toString()

        currentItinerary = Itinerary(
            name = name,
            ville_depart = ville,
            date_debut = dateFormatted,
            isPublic = isPublic,
            countryCode = countryCode
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
            prepareVoyageAndSave(userId, voyageId, itinerary, firestorePoints, uploadedPhotos, context)
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
                    date = getPhotoDateFromExif(context, uri),
                    latitude = photo.position?.latitude ?: 0.0,
                    longitude = photo.position?.longitude ?: 0.0,
                    commentaire = photo.attachedPoiName ?: "",
                    utilisateur = userId
                )
                uploadedPhotos.add(meta)
                uploadCount++
                if (uploadCount == totalPhotos) {
                    prepareVoyageAndSave(userId, voyageId, itinerary, firestorePoints, uploadedPhotos, context)
                }
            }.addOnFailureListener {
                uploadCount++
                if (uploadCount == totalPhotos) {
                    prepareVoyageAndSave(userId, voyageId, itinerary, firestorePoints, uploadedPhotos, context)
                }
            }
        }
    }

    private fun prepareVoyageAndSave(
        userId: String,
        voyageId: String,
        itinerary: Itinerary,
        points: List<com.google.firebase.firestore.GeoPoint>,
        photos: List<PhotoMeta>,
        context: Context
    ) {

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Utilisateurs").document(userId)
        val voyageRef = userRef.collection("voyages").document(voyageId)
        val poisCol = voyageRef.collection("poi")

        val voyage = Voyage(
            id = voyageId,
            nom = itinerary.name ?: "Voyage sans nom",
            description = "",
            villeDepart = itinerary.ville_depart ?: "",
            dateDebut = itinerary.date_debut ?: "",
            dateFin = itinerary.date_fin ?: "",
            points = points,
            photos = photos,
            utilisateur = userId,
            isTripPublic = itinerary.isPublic,
            countryCode = itinerary.countryCode
        )

// 2) Récupérer les infos user, puis lancer le batch
        userRef.get().continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception!!
            val snap = task.result!!
            voyage.ownerFirstName = snap.getString("firstName").orEmpty()
            voyage.ownerLastName = snap.getString("lastName").orEmpty()
            voyage.ownerPhotoUrl = snap.getString("profilePhotoUrl").orEmpty()

            // 3) Préparer un batch
            val batch = db.batch()
            // 3.a) Écrire le document Voyage
            batch.set(voyageRef, voyage)
            // 3.b) Pour chacun des POI, ajouter un set() dans la sous-collection "poi"
            itinerary.interst_points.forEach { poi ->
                val poiDoc = poisCol.document()
                batch.set(
                    poiDoc, mapOf(
                        "name" to poi.name,
                        "description" to poi.description,
                        "latitude" to poi.location.latitude,
                        "longitude" to poi.location.longitude,
                    )
                )
            }
            // 4) Commit unique
            batch.commit()
        }
            .addOnSuccessListener {
                Toast.makeText(context, "Voyage + POI sauvegardés avec succès", Toast.LENGTH_SHORT)
                    .show()
                currentItinerary = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur sauvegarde : ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                Log.e("Firebase", "Erreur batch save: ${e.message}")
            }
    }

    private fun getPhotoDateFromExif(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.use { ExifInterface(it) }
            val dateString = exif?.getAttribute(ExifInterface.TAG_DATETIME)
            val exifFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            dateString?.let {
                val parsedDate = exifFormat.parse(it)
                parsedDate?.let { outputFormat.format(it) } ?: outputFormat.format(Date())
            } ?: outputFormat.format(Date())
        } catch (e: Exception) {
            e.printStackTrace()
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        }
    }
}
