package com.example.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import androidx.core.widget.doOnTextChanged


class JourneyFragment : Fragment() {

    private var _binding: FragmentJourneyBinding? = null
    private val binding get() = _binding!!

    private lateinit var voyageAdapter: VoyageAdapterPublic
    private val publicVoyagesList = mutableListOf<Voyage>()
    private val displayedVoyages = mutableListOf<Voyage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Récupération du binding à partir du layout
        _binding = FragmentJourneyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Initialisation du RecyclerView
        binding.recyclerViewJourney.layoutManager = LinearLayoutManager(requireContext())
        voyageAdapter = VoyageAdapterPublic(
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
        Log.d("DEBUG", "onViewCreated appelé")

        loadPublicTrips()

        // Lorsque l'utilisateur tape quelque chose dans la barre de recherche
        binding.editTextJourney.doOnTextChanged { text, _, _, _ ->
            // On récupère ce qu'il a entré
            val query = text?.toString()?.trim()?.lowercase() ?: ""
            // On crée une nouvelle liste filtré à partir de la première avec les ville qui contiennent
            // La chaine de caractères que l'utilisateur a entré
            val filteredList = publicVoyagesList.filter {
                it.villeDepart?.lowercase()?.contains(query) == true
            }
            updateDisplayedVoyages(filteredList)
        }



    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadPublicTrips() {
        FirebaseFirestore.getInstance()
            .collectionGroup("voyages")
            .whereEqualTo("tripPublic", true)
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

                updateDisplayedVoyages(publicVoyagesList)
            }
            .addOnFailureListener {
                Log.e("DEBUG", "Erreur Firebase : ${it.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateDisplayedVoyages(list: List<Voyage>) {

        displayedVoyages.clear()
        displayedVoyages.addAll(list)
        voyageAdapter.updateList(displayedVoyages)

        // Si la liste est vide : on affiche le emptyText
        binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        // Sinon on affiche les voyages concernés
        binding.recyclerViewJourney.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

}
