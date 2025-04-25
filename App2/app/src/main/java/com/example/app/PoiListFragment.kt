package com.example.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.managers.PoiAdapter
import com.example.app.managers.PoiManager
import com.example.app.models.PoiModel
import com.google.firebase.auth.FirebaseAuth

class PoiListFragment : Fragment() {

    companion object {
        private const val ARG_VOYAGE_ID = "voyageId"
        private const val ARG_OWNER_ID  = "ownerId"

        /** Utilisez toujours newInstance(voyageId, ownerId) pour créer ce fragment */
        fun newInstance(voyageId: String, ownerId: String) = PoiListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_VOYAGE_ID, voyageId)
                putString(ARG_OWNER_ID, ownerId)
            }
        }
    }

    private lateinit var rv: RecyclerView
    private lateinit var emptyTv: TextView
    private lateinit var addBtn: ImageButton
    private lateinit var adapter: PoiAdapter
    private val pois = mutableListOf<PoiModel>()

    // ID du voyage dont on affiche les POI
    private lateinit var voyageId: String
    // ID du propriétaire du voyage (la racine /Utilisateurs/{ownerId}/voyages)
    private lateinit var ownerId: String
    // ID de l'utilisateur actuellement connecté
    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voyageId = requireArguments().getString(ARG_VOYAGE_ID) ?: ""
        ownerId  = requireArguments().getString(ARG_OWNER_ID)  ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_poi_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Bind des vues
        rv      = view.findViewById(R.id.recyclerViewPois)
        emptyTv = view.findViewById(R.id.textEmptyPois)
        addBtn  = view.findViewById(R.id.btnAddPoi)

        // 2) On n’affiche le + que si on est le propriétaire
        if (currentUserId == ownerId) {
            addBtn.visibility = View.VISIBLE
            addBtn.setOnClickListener { showPoiDialog(null) }
        } else {
            addBtn.visibility = View.GONE
        }

        // 3) Setup RecyclerView
        adapter = PoiAdapter(pois,
            onClick     = { /* TODO: click sur un POI existant */ },
            onLongClick = { /* TODO: long-click pour éditer/supprimer */ }
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter       = adapter

        // 4) Chargement initial
        loadPois()
    }

    /** Charge les POIs depuis /Utilisateurs/{ownerId}/voyages/{voyageId}/pois */
    private fun loadPois() {
        if (ownerId.isEmpty() || voyageId.isEmpty()) return

        PoiManager.fetchPois(
            userId   = ownerId,    // <–– on passe ownerId ici
            voyageId = voyageId,
            onSuccess = { list ->
                pois.clear()
                pois.addAll(list)
                adapter.notifyDataSetChanged()
                updateEmptyState()
            },
            onFailure = {
                Toast.makeText(requireContext(),
                    "Erreur chargement POIs", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
        )
    }

    private fun updateEmptyState() {
        if (pois.isEmpty()) {
            rv.visibility      = View.GONE
            emptyTv.visibility = View.VISIBLE
        } else {
            rv.visibility      = View.VISIBLE
            emptyTv.visibility = View.GONE
        }
    }

    /**
     * Affiche le dialogue d’ajout/édition.
     * Si existing == null ⇒ on ajoute un nouveau POI.
     * Sinon ⇒ on met à jour.
     */
    private fun showPoiDialog(existing: PoiModel?) {
        val dv        = layoutInflater.inflate(R.layout.dialog_edit_poi, null)
        val editName  = dv.findViewById<EditText>(R.id.editPoiName)
        val editCom   = dv.findViewById<EditText>(R.id.editPoiComments)

        if (existing != null) {
            editName.setText(existing.name)
            editCom.setText(existing.commentaires)
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(
                if (existing == null) getString(R.string.add_poi)
                else                   getString(R.string.edit_poi)
            )
            .setView(dv)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = editName.text.toString().trim()
                val com  = editCom.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Le nom du POI ne peut pas être vide",
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existing == null) {
                    // Création
                    val newPoi = PoiModel(
                        name       = name,
                        commentaires = com,
                        createdBy  = currentUserId
                    )
                    PoiManager.addPoi(
                        userId   = ownerId,    // <–– on crée dans la collection du propriétaire
                        voyageId = voyageId,
                        poi      = newPoi,
                        onSuccess = { loadPois() },
                        onFailure = { e -> Toast.makeText(requireContext(),
                            "Erreur ajout POI: ${e.message}",
                            Toast.LENGTH_LONG).show() }
                    )
                } else {
                    // Mise à jour
                    val updates = mapOf(
                        "name"         to name,
                        "commentaires" to com
                    )
                    PoiManager.updatePoi(
                        userId   = ownerId,
                        voyageId = voyageId,
                        poiId    = existing.id,
                        updates  = updates,
                        onSuccess = { loadPois() },
                        onFailure = { e -> Toast.makeText(requireContext(),
                            "Erreur maj POI: ${e.message}",
                            Toast.LENGTH_LONG).show() }
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)

        // bouton “Supprimer” uniquement en édition
        existing?.let {
            builder.setNeutralButton(R.string.delete_poi) { _, _ ->
                PoiManager.deletePoi(
                    userId   = ownerId,
                    voyageId = voyageId,
                    poiId    = it.id,
                    onSuccess = { loadPois() },
                    onFailure = { e -> Toast.makeText(requireContext(),
                        "Erreur suppression: ${e.message}",
                        Toast.LENGTH_LONG).show() }
                )
            }
        }

        builder.show()
    }
}
