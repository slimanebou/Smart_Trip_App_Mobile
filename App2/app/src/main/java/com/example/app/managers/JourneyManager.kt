package com.example.app.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.app.models.*
import com.example.app.service.GpsTrackingService
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.util.GeoPoint
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

        saveVoyageToFirestore(userId, voyage)
            .addOnSuccessListener {
                Toast.makeText(context, "Voyage enregistré avec succès", Toast.LENGTH_SHORT).show()
                Log.d("Firebase", "✅ Voyage sauvegardé avec infos utilisateur")
                currentItinerary = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "❌ Erreur Firestore : ${e.message}")
            }
    }

    fun saveVoyageToFirestore(userId: String, voyage: Voyage): Task<Void> {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("Utilisateurs").document(userId)

        return userDocRef.get().continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: Exception("Échec de la récupération de l'utilisateur")
            }

            val userSnap = task.result
            val firstName = userSnap?.getString("firstName") ?: ""
            val lastName = userSnap?.getString("lastName") ?: ""
            val profileUrl = userSnap?.getString("profilePhotoUrl") ?: ""

            voyage.ownerFirstName = firstName
            voyage.ownerLastName = lastName
            voyage.ownerPhotoUrl = profileUrl

            userDocRef.collection("voyages").document(voyage.id).set(voyage)
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
