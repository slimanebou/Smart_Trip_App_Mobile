package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.managers.VoyageAdapterPublic
import com.example.app.models.Voyage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoriteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VoyageAdapterPublic
    private val favorisList = mutableListOf<Voyage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Configuration du RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewFavorite)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = VoyageAdapterPublic() { voyage ->
            // Clic sur un favori → Détails
            val details = VoyageDetailsFragment().apply {
                arguments = Bundle().apply { putSerializable("voyage", voyage) }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, details)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        // 2) Lecture des favoris depuis Firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("Utilisateurs")
            .document(userId)
            .collection("favoris")
            .get()
            .addOnSuccessListener { favSnap ->
                // On vide d’abord la liste locale pour éviter doublons ou éléments obsolètes
                favorisList.clear()

                // Pour chaque doc "favoris"
                favSnap.documents.forEach { favDoc ->
                    val tripId = favDoc.getString("tripId")
                    val ownerId = favDoc.getString("ownerId")
                    val favRef = favDoc.reference

                    if (tripId == null || ownerId == null) {
                        // Données malformées : on supprime l’entry orpheline
                        favRef.delete()
                        return@forEach
                    }

                    // Vérification de l’existence du voyage
                    db.collection("Utilisateurs")
                        .document(ownerId)
                        .collection("voyages")
                        .document(tripId)
                        .get()
                        .addOnSuccessListener { tripDoc ->
                            if (tripDoc.exists()) {
                                // Voyage valide → on l’ajoute à l’affichage
                                tripDoc.toObject(Voyage::class.java)?.let { voyage ->
                                    voyage.id = tripDoc.id
                                    voyage.utilisateur = ownerId
                                    favorisList.add(voyage)
                                    adapter.updateList(favorisList)
                                }
                            } else {
                                // Voyage supprimé → on nettoie le favori orphelin
                                favRef.delete()
                            }
                        }
                        .addOnFailureListener {
                            // Optionnel : log ou Toast
                        }
                }
            }
            .addOnFailureListener {
                // Gérer l’erreur de lecture de la sous-collection “favoris”
            }
    }
}