package com.example.app.managers

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.models.UserInfo
import com.example.app.models.Voyage
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VoyageAdapterPublic(
    private val voyages: List<Voyage>,
    private val onItemClick: (Voyage) -> Unit
) : RecyclerView.Adapter<VoyageAdapterPublic.VoyageViewHolder>() {

    inner class VoyageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageVoyage: ImageView = itemView.findViewById(R.id.imageVoyage)
        val titleText: TextView = itemView.findViewById(R.id.textTitle)
        val datesText: TextView = itemView.findViewById(R.id.textDate)
        val profileName: TextView = itemView.findViewById(R.id.profileName)
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)
        val textCity : TextView = itemView.findViewById(R.id.textCity)

        @SuppressLint("SetTextI18n")
        fun bind(voyage: Voyage) {
            titleText.text = voyage.nom
            val formattedDates = itemView.context.getString(
                R.string.trip_dates,
                formatDateString(voyage.dateDebut),
                formatDateString(voyage.dateFin)
            )
            datesText.text = formattedDates
            val countryCode = voyage.countryCode ?: ""
            val city = voyage.villeDepart ?: "Ville"
            val flag = countryCodeToFlag(countryCode)
            textCity.text = "$flag $city"

            val imageUrl = voyage.coverPhotoUrl ?: voyage.photos.firstOrNull()?.url
            if (imageUrl != null) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(imageVoyage)
            } else {
                imageVoyage.setImageResource(R.drawable.image_placeholder)
            }

            // Chargement des infos utilisateur
            val fullName = "${voyage.ownerFirstName ?: ""} ${voyage.ownerLastName ?: ""}".trim()
            profileName.text = if (fullName.isNotBlank()) fullName else "(Utilisateur)"

            Glide.with(itemView.context)
                .load(voyage.ownerPhotoUrl)
                .placeholder(R.drawable.user_1)
                .circleCrop()
                .into(profileImage)


            // Actions
            imageVoyage.setOnClickListener { onItemClick(voyage) }

            // Favoris par défaut en blanc (pas actif ici)
            favoriteIcon.setImageResource(R.drawable.favorite_white)


            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val favRef = FirebaseFirestore.getInstance()
                    .collection("Utilisateurs")
                    .document(userId)
                    .collection("favoris")
                    .document(voyage.id)

                // 1) Afficher état initial du cœur
                favRef.get().addOnSuccessListener { doc ->
                    val isFav = doc.exists()
                    favoriteIcon.setImageResource(
                        if (isFav) R.drawable.favorite else R.drawable.favorite_white
                    )
                }
// 2) Clic sur le cœur → toggle
                favoriteIcon.setOnClickListener {
                    favRef.get().addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            // déjà favori → on supprime
                            favRef.delete()
                                .addOnSuccessListener {
                                    favoriteIcon.setImageResource(R.drawable.favorite_white)
                                }
                        } else {
                            // pas favori → on ajoute
                            favRef.set(
                                mapOf(
                                    "tripId" to voyage.id,
                                    "ownerId" to voyage.utilisateur
                                ))
                                .addOnSuccessListener {
                                    favoriteIcon.setImageResource(R.drawable.favorite)
                                }
                        }
                    }
                }
    }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoyageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voyage, parent, false)
        return VoyageViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoyageViewHolder, position: Int) {
        holder.bind(voyages[position])
    }

    override fun getItemCount(): Int = voyages.size

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

    private fun countryCodeToFlag(code: String): String {
        return code.uppercase()
            .map { char -> Character.toChars(0x1F1E6 - 'A'.code + char.code).concatToString() }
            .joinToString("")
    }

}