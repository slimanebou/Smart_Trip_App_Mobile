package com.example.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.managers.VoyageAdapter
import com.example.app.models.Voyage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.app.databinding.FragmentMyTripsBinding
import com.example.app.databinding.FragmentProfileBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MyTripsFragment : Fragment() {

    // Déclaration un variable mutable binding pour FragmentProfileBinding pour gerer les cycles de vie
    // _ c'est une convention Kotlin pour indique la version brute (comme * en rust)
    private var _binding: FragmentMyTripsBinding? = null


    // Déclaration une variable immutable (propriété en lecture seule) binding pour FragmentProfileBinding pour gerer les cycles de vie
    private val binding get() = _binding!!


    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VoyageAdapter
    private val voyages = mutableListOf<Voyage>()


    // function
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTripsBinding.inflate(inflater, container, false)

        recyclerView = binding.recyclerViewVoyages
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = VoyageAdapter(voyages,
            onItemClick = { voyage ->
                // Quand on clique sur la carte entière
                val fragment = VoyageDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("voyage", voyage)
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onImageClick = { voyage ->
                // 👉 Ouvre un sélecteur d’image ici
                Toast.makeText(
                    requireContext(),
                    "Changer l'image pour ${voyage.nom}",
                    Toast.LENGTH_SHORT
                ).show()
                val photoUrls = voyage.photos.map { it.url }

                AlertDialog.Builder(requireContext())
                    .setTitle("Choisir une image de couverture")
                    .setItems(photoUrls.mapIndexed { i, url -> "Photo ${i + 1}" }
                        .toTypedArray()) { _, index ->
                        val chosenUrl = photoUrls[index]
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setItems
                        FirebaseFirestore.getInstance()
                            .collection("Utilisateurs").document(userId)
                            .collection("voyages").document(voyage.id)
                            .update("coverPhotoUrl", chosenUrl)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Image de couverture mise à jour",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("Annuler", null)
                    .show()

            }
        )

        recyclerView.adapter = adapter
        loadVoyagesFromFirestore()

        return binding.root
    }



    private fun loadVoyagesFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("Utilisateurs").document(userId)
            .collection("voyages")
            .get()
            .addOnSuccessListener { result ->
                voyages.clear()
                for (doc in result) {
                    val voyage = doc.toObject(Voyage::class.java)
                    voyages.add(voyage)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e -> e.printStackTrace() }
    }
}
