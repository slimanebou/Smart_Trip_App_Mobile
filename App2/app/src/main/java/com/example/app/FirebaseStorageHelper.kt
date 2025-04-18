package com.example.app

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.GeoPoint
import java.util.*

object FirebaseStorageHelper {

    fun uploadProfilePhoto(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return onFailure(Exception("Utilisateur non connecté"))

        val storageRef = FirebaseStorage.getInstance().reference
            .child("users/$userId/profile.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                    // ✅ Mise à jour dans Realtime Database
                    val database = FirebaseDatabase.getInstance()
                    val usersRef = database.getReference("users")

                    usersRef.child(userId).child("profilePhotoUrl")
                        .setValue(downloadUrl.toString())
                        .addOnSuccessListener { onSuccess(downloadUrl.toString()) }
                        .addOnFailureListener { onFailure(it) }

                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun uploadPhotoForVoyage(
        imageUri: Uri,
        voyageId: String,
        lat: Double,
        lng: Double,
        description: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return onFailure(Exception("Utilisateur non connecté"))

        val fileName = UUID.randomUUID().toString() + ".jpg"
        val photoRef = FirebaseStorage.getInstance().reference
            .child("photos/$userId/$voyageId/$fileName")

        photoRef.putFile(imageUri)
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val firestore = FirebaseFirestore.getInstance()
                    val entree = hashMapOf(
                        "type" to "photo",
                        "photo_url" to downloadUrl.toString(),
                        "description" to description,
                        "timestamp" to Timestamp.now(),
                        "position" to GeoPoint(lat, lng)
                    )

                    firestore.collection("Utilisateurs")
                        .document(userId)
                        .collection("voyages")
                        .document(voyageId)
                        .collection("entrées")
                        .add(entree)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}
