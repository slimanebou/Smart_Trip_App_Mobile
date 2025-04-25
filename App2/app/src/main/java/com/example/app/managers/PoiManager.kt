package com.example.app.managers

import com.example.app.models.PoiModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

object PoiManager {
    private val db = FirebaseFirestore.getInstance()

    private fun poisCollection(userId: String, voyageId: String) =
        db.collection("Utilisateurs")
            .document(userId)
            .collection("voyages")
            .document(voyageId)
            .collection("pois")

    /** Charge tous les POIs pour ce voyage */
    fun fetchPois(
        userId: String, voyageId: String,
        onSuccess: (List<PoiModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        poisCollection(userId, voyageId)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map { d ->
                    d.toObject(PoiModel::class.java)!!.apply { id = d.id }
                }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /** Crée un nouveau POI */
    fun addPoi(
        userId: String, voyageId: String, poi: PoiModel,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        poisCollection(userId, voyageId)
            .add(poi)
            .addOnSuccessListener { ref -> onSuccess(ref.id) }
            .addOnFailureListener { onFailure(it) }
    }

    /** Met à jour un POI existant */
    fun updatePoi(
        userId: String, voyageId: String, poiId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        poisCollection(userId, voyageId)
            .document(poiId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Supprime un POI */
    fun deletePoi(
        userId: String, voyageId: String, poiId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        poisCollection(userId, voyageId)
            .document(poiId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
