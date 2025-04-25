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
import androidx.appcompat.widget.SwitchCompat

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


            },
                    onEditClick = { voyage -> showEditDialog(voyage) }
        )

        recyclerView.adapter = adapter
        loadVoyages(userId)

        return binding.root
    }

    private fun loadVoyages(userId: String) {
        FirebaseFirestore.getInstance()
            .collection("Utilisateurs")
            .document(userId)
            .collection("voyages")
            .get()
            .addOnSuccessListener { result ->
                voyages.clear()
                for (doc in result) {
                    // Transforme en Voyage ET injecte l'owner et l'id
                    val voyage = doc.toObject(Voyage::class.java).apply {
                        id = doc.id
                        utilisateur = userId
                    }
                    voyages.add(voyage)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(requireContext(),
                    "Erreur de chargement des voyages",
                    Toast.LENGTH_SHORT).show()
            }
    }


    private fun showEditDialog(voyage: Voyage) {
        // 1) Inflater le layout de dialogue (à créer en res/layout/dialog_edit_voyage.xml)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_voyage, null)

        // 2) Pré-remplir les champs
        dialogView.findViewById<EditText>(R.id.editName)
            .setText(voyage.nom)
        dialogView.findViewById<EditText>(R.id.editDescription)
            .setText(voyage.description)
        dialogView.findViewById<EditText>(R.id.editCountryCode)
            .setText(voyage.countryCode)
        dialogView.findViewById<EditText>(R.id.editCity)
            .setText(voyage.villeDepart)
        dialogView.findViewById<SwitchCompat>(R.id.switchPublic)
            .isChecked = voyage.isTripPublic

        // 3) Construire et afficher l'AlertDialog
        AlertDialog.Builder(requireContext())
            .setTitle("Modifier le voyage")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { _, _ ->
                // 4) Récupérer les nouvelles valeurs
                val newName = dialogView.findViewById<EditText>(R.id.editName).text.toString()
                val newDesc = dialogView.findViewById<EditText>(R.id.editDescription).text.toString()
                val newCountry = dialogView.findViewById<EditText>(R.id.editCountryCode).text.toString()
                val newCity = dialogView.findViewById<EditText>(R.id.editCity).text.toString()
                val newPublic = dialogView.findViewById<SwitchCompat>(R.id.switchPublic).isChecked

                // 5) Mettre à jour dans Firestore
                FirebaseFirestore.getInstance()
                    .collection("Utilisateurs")
                    .document(voyage.utilisateur)
                    .collection("voyages")
                    .document(voyage.id)
                    .update(
                        "nom", newName,
                        "description", newDesc,
                        "countryCode", newCountry,
                        "villeDepart", newCity,
                        "tripPublic", newPublic
                    )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
