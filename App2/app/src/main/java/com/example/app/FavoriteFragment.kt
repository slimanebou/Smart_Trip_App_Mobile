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
                // On récupère tripId + ownerId depuis chaque doc "favoris"
                val pairs = favSnap.documents.mapNotNull { doc ->
                    val tripId  = doc.getString("tripId")
                    val ownerId = doc.getString("ownerId")
                    if (tripId != null && ownerId != null) tripId to ownerId else null
                }

                favorisList.clear()
                // Pour chaque (tripId, ownerId), on va chercher le document précis
                pairs.forEach { (tripId, ownerId) ->
                    db.collection("Utilisateurs")
                        .document(ownerId)
                        .collection("voyages")
                        .document(tripId)
                        .get()
                        .addOnSuccessListener { tripDoc ->
                            tripDoc.toObject(Voyage::class.java)?.let { voyage ->
                                voyage.id = tripDoc.id
                                favorisList.add(voyage)
                                adapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener {
                            // Gérer l'erreur si besoin (log / Toast)
                        }
                }
            }
            .addOnFailureListener {
                // Gérer l'erreur de lecture de la sous-collection "favoris"
            }
    }
}
