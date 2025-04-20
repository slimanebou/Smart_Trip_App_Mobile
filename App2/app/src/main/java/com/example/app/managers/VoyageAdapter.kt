package com.example.app.managers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.VoyageDetailsFragment
import com.example.app.models.Voyage
import java.io.Serializable


class VoyageAdapter(
    private val voyages: List<Voyage>,
    private val onVoyageClick: (Voyage) -> Unit
) : RecyclerView.Adapter<VoyageAdapter.VoyageViewHolder>() {

    inner class VoyageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textNomVoyage: TextView = itemView.findViewById(R.id.textNomVoyage)

        fun bind(voyage: Voyage) {
            textNomVoyage.text = voyage.nom
            itemView.setOnClickListener {
                onVoyageClick(voyage) // 👉 seulement ça
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
}
