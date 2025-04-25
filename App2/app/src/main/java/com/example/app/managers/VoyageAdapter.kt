package com.example.app.managers

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.models.Voyage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class VoyageAdapter(
    private val voyages: List<Voyage>,
    private val onItemClick: (Voyage) -> Unit,
    private val onImageClick: (Voyage) -> Unit,
    private val onEditClick: (Voyage) -> Unit
) : RecyclerView.Adapter<VoyageAdapter.VoyageViewHolder>() {

    inner class VoyageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // --- vues existantes ---
        private val imageVoyage: ImageView    = itemView.findViewById(R.id.imageVoyage)
        private val titleText: TextView       = itemView.findViewById(R.id.textTitle)
        private val datesText: TextView       = itemView.findViewById(R.id.textDate)
        private val favoriteIcon: ImageView   = itemView.findViewById(R.id.favoriteIcon)
        private val editButton: ImageButton   = itemView.findViewById(R.id.btnEditTrip)

        // --- NOUVEAU : vues profil & ville/drapeau ---
        private val profileImage: ImageView   = itemView.findViewById(R.id.profileImage)
        private val profileName: TextView     = itemView.findViewById(R.id.profileName)
        private val textCity: TextView        = itemView.findViewById(R.id.textCity)

        fun bind(voyage: Voyage) {
            // Titre & dates
            titleText.text = voyage.nom
            datesText.text  = itemView.context.getString(
                R.string.trip_dates,
                formatDateString(voyage.dateDebut),
                formatDateString(voyage.dateFin)
            )

            // COUVERTURE
            val imageUrl = voyage.coverPhotoUrl ?: voyage.photos.firstOrNull()?.url
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .centerCrop()
                    .into(imageVoyage)
            } else {
                imageVoyage.setImageResource(R.drawable.image_placeholder)
            }

            // CLICS sur image et titre
            imageVoyage.setOnClickListener { onItemClick(voyage) }
            titleText.setOnClickListener { onImageClick(voyage) }

            // --- NOUVEAU : afficher ville + drapeau ---
            val countryCode = voyage.countryCode ?: ""
            val city        = voyage.villeDepart ?: ""
            textCity.text   = "${countryCodeToFlag(countryCode)} $city"

            // --- NOUVEAU : afficher profil du propriétaire ---
            val fullName = listOfNotNull(voyage.ownerFirstName, voyage.ownerLastName)
                .joinToString(" ")
                .ifBlank { "(You)" }
            profileName.text = fullName

            Glide.with(itemView.context)
                .load(voyage.ownerPhotoUrl)
                .placeholder(R.drawable.user_1)
                .circleCrop()
                .into(profileImage)

            // FAVORIS
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid != null) {
                val favRef = FirebaseFirestore.getInstance()
                    .collection("Utilisateurs")
                    .document(currentUid)
                    .collection("favoris")
                    .document(voyage.id)

                // état initial
                favRef.get().addOnSuccessListener { doc ->
                    favoriteIcon.setImageResource(
                        if (doc.exists()) R.drawable.favorite
                        else              R.drawable.favorite_white
                    )
                }
                // toggle au clic
                favoriteIcon.setOnClickListener {
                    favRef.get().addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            favRef.delete().addOnSuccessListener {
                                favoriteIcon.setImageResource(R.drawable.favorite_white)
                            }
                        } else {
                            favRef.set(mapOf(
                                "tripId" to voyage.id,
                                "ownerId" to voyage.utilisateur
                            )).addOnSuccessListener {
                                favoriteIcon.setImageResource(R.drawable.favorite)
                            }
                        }
                    }
                }
            } else {
                favoriteIcon.setOnClickListener {
                    Toast.makeText(itemView.context,
                        "Please log in to manage favorites",
                        Toast.LENGTH_SHORT).show()
                }
            }

            // BOUTON ÉDITION
            editButton.setOnClickListener { onEditClick(voyage) }
        }

        private fun formatDateString(dateStr: String?): String {
            if (dateStr.isNullOrBlank()) return "?"
            return try {
                val parser    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = parser.parse(dateStr)
                formatter.format(date!!)
            } catch (_: Exception) {
                "?"
            }
        }

        // --- NOUVEAU : utilitaire drapeau à partir du countryCode ---
        private fun countryCodeToFlag(code: String): String {
            return code.uppercase()
                .map { char ->
                    Character.toChars(0x1F1E6 - 'A'.code + char.code).concatToString()
                }
                .joinToString("")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoyageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_trip, parent, false)
        return VoyageViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoyageViewHolder, position: Int) {
        holder.bind(voyages[position])
    }

    override fun getItemCount(): Int = voyages.size
}
