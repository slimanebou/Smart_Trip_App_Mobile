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

class JourneyFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VoyageAdapter
    private val voyages = mutableListOf<Voyage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_journey, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewVoyages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = VoyageAdapter(voyages) { voyage ->
            val fragment = VoyageDetailsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("voyage", voyage)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.adapter = adapter
        loadVoyagesFromFirestore()

        return view
    }

    private fun loadVoyagesFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("Utilisateurs").document(userId)
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
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}
