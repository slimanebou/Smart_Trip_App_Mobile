package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.databinding.FragmentJourneyBinding
import com.example.app.managers.VoyageAdapterPublic
import com.example.app.models.Voyage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class JourneyFragment : Fragment() {

    private var _binding: FragmentJourneyBinding? = null
    private val binding get() = _binding!!

    private lateinit var voyageAdapter: VoyageAdapterPublic
    private val publicVoyagesList = mutableListOf<Voyage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJourneyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Initialisation du RecyclerView
        binding.recyclerViewJourney.layoutManager = LinearLayoutManager(requireContext())
        voyageAdapter = VoyageAdapterPublic(
            voyages = publicVoyagesList,
            onItemClick = { voyage ->
                // 2) Lorsqu'on clique sur un voyage, on remplace par VoyageDetailsFragment
                val details = VoyageDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("voyage", voyage)
                    }
                }
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, details)
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.recyclerViewJourney.adapter = voyageAdapter

        // 3) Chargement des voyages publics
        loadPublicTrips()
    }

    private fun loadPublicTrips() {
        FirebaseFirestore.getInstance()
            .collectionGroup("voyages")
            .whereEqualTo("public", true)
            .orderBy("dateDebut", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                publicVoyagesList.clear()
                for (doc in snapshot.documents) {
                    doc.toObject(Voyage::class.java)?.apply {
                        id = doc.id
                        publicVoyagesList.add(this)
                    }
                }
                voyageAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
