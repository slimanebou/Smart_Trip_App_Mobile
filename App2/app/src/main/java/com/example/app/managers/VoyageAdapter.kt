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
        private val imageVoyage: ImageView = itemView.findViewById(R.id.imageVoyage)
        private val titleText: TextView = itemView.findViewById(R.id.textTitle)
        private val datesText: TextView = itemView.findViewById(R.id.textDate)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)

        fun bind(voyage: Voyage) {
            // Titre et dates
            titleText.text = voyage.nom
            datesText.text = itemView.context.getString(
                R.string.trip_dates,
                formatDateString(voyage.dateDebut),
                formatDateString(voyage.dateFin)
            )


            // Image de couverture
            val imageUrl = voyage.coverPhotoUrl ?: voyage.photos.firstOrNull()?.url
            if (imageUrl != null) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .centerCrop()
                    .into(imageVoyage)
            } else {
                imageVoyage.setImageResource(R.drawable.image_placeholder)
            }

            // Clics sur image et titre
            imageVoyage.setOnClickListener { onItemClick(voyage) }
            titleText.setOnClickListener { onImageClick(voyage) }
            titleText.apply {
                setTextColor(ContextCompat.getColor(context, R.color.purple_500))
                paint.isUnderlineText = true
            }

            // Gestion du favori via Firestore
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val favRef = FirebaseFirestore.getInstance()
                    .collection("Utilisateurs")
                    .document(userId)
                    .collection("favoris")
                    .document(voyage.id)

                // Affiche l’état initial du cœur
                favRef.get().addOnSuccessListener { doc ->
                    val isFav = doc.exists()
                    favoriteIcon.setImageResource(
                        if (isFav) R.drawable.favorite
                        else       R.drawable.favorite_white
                    )
                }

                // Toggle favori au clic
                favoriteIcon.setOnClickListener {
                    favRef.get().addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            // Supprimer des favoris
                            favRef.delete()
                                .addOnSuccessListener {
                                    favoriteIcon.setImageResource(R.drawable.favorite_white)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(itemView.context,
                                        "Erreur suppression favori",
                                        Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // Ajouter aux favoris
                            favRef.set(mapOf(
                                "tripId" to voyage.id,
                                "ownerId" to voyage.utilisateur
                            )).addOnSuccessListener {
                                favoriteIcon.setImageResource(R.drawable.favorite)
                            }.addOnFailureListener {
                                Toast.makeText(itemView.context,
                                    "Erreur ajout favori",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                // Pas connecté, proposer de se loguer
                favoriteIcon.setOnClickListener {
                    Toast.makeText(itemView.context,
                        "Connectez-vous pour gérer vos favoris",
                        Toast.LENGTH_SHORT).show()
                }
            }

            itemView.findViewById<ImageButton>(R.id.btnEditTrip)
                .setOnClickListener { onEditClick(voyage) }
        }

        private fun formatDateString(dateStr: String?): String {
            if (dateStr.isNullOrBlank()) return "?"
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = parser.parse(dateStr)
                formatter.format(date!!)
            } catch (e: Exception) {
                "?"
            }
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
