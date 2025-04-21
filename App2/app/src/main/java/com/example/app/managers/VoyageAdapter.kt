package com.example.app.managers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.models.Voyage
import java.text.SimpleDateFormat
import java.util.*

class VoyageAdapter(
    private val voyages: List<Voyage>,
    private val onItemClick: (Voyage) -> Unit,
    private val onImageClick: (Voyage) -> Unit
) : RecyclerView.Adapter<VoyageAdapter.VoyageViewHolder>() {

    inner class VoyageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageVoyage: ImageView = itemView.findViewById(R.id.imageVoyage)
        val titleText: TextView = itemView.findViewById(R.id.textTitle)
        val datesText: TextView = itemView.findViewById(R.id.textDate)

        fun bind(voyage: Voyage) {
            titleText.text = voyage.nom

            val formattedDates = itemView.context.getString(
                R.string.trip_dates,
                formatDateString(voyage.dateDebut),
                formatDateString(voyage.dateFin)
            )
            datesText.text = formattedDates

            val imageUrl = voyage.coverPhotoUrl ?: voyage.photos.firstOrNull()?.url
            if (imageUrl != null) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(imageVoyage)
            } else {
                imageVoyage.setImageResource(R.drawable.image_placeholder)
            }

            // ✅ Nouvelle logique
            imageVoyage.setOnClickListener {
                onItemClick(voyage) // Ouvre les détails du voyage
            }

            titleText.setOnClickListener {
                onImageClick(voyage) // Ouvre la sélection d’image
            }

            // Style du titre → pour indiquer que c’est cliquable
            titleText.apply {
                setTextColor(ContextCompat.getColor(context, R.color.purple_500))
                paint.isUnderlineText = true
            }

            // Désactiver clic sur la carte complète
            itemView.setOnClickListener(null)
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
}
