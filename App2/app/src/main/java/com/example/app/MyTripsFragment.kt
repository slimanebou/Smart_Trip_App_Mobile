package com.example.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.databinding.FragmentMyTripsBinding
import com.example.app.managers.VoyageAdapter
import com.example.app.models.Voyage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyTripsFragment : Fragment() {

    private var _binding: FragmentMyTripsBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VoyageAdapter
    private val voyages = mutableListOf<Voyage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTripsBinding.inflate(inflater, container, false)

        recyclerView = binding.recyclerViewVoyages
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return binding.root

        // 🔄 Initialisation de l'adapter (plus besoin de charger l'utilisateur séparément)
        adapter = VoyageAdapter(
            voyages,
            onItemClick = { voyage ->
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
                val photoUrls = voyage.photos.map { it.url }
                AlertDialog.Builder(requireContext())
                    .setTitle("Choisir une image de couverture")
                    .setItems(photoUrls.mapIndexed { i, _ -> "Photo ${i + 1}" }.toTypedArray()) { _, index ->
                        val chosenUrl = photoUrls[index]
                        FirebaseFirestore.getInstance()
                            .collection("Utilisateurs").document(userId)
                            .collection("voyages").document(voyage.id)
                            .update("coverPhotoUrl", chosenUrl)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Image de couverture mise à jour", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        )

        recyclerView.adapter = adapter
        loadVoyages(userId)

        return binding.root
    }

    private fun loadVoyages(userId: String) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
