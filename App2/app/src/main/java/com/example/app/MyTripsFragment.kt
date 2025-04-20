package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.managers.VoyageAdapter
import com.example.app.models.Voyage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast


class MyTripsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VoyageAdapter
    private val voyages = mutableListOf<Voyage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_my_trips, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewVoyages)
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
                Toast.makeText(requireContext(), "Changer l'image pour ${voyage.nom}", Toast.LENGTH_SHORT).show()
                // Tu peux ici démarrer une Activity pour sélectionner une image (ACTION_PICK)
            }
        )

        recyclerView.adapter = adapter
        loadVoyagesFromFirestore()

        return view
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
