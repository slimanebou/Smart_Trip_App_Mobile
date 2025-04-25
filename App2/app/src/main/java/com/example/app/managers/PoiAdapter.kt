package com.example.app.managers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.models.PoiModel

class PoiAdapter(
    private val pois: List<PoiModel>,
    private val onClick: (PoiModel) -> Unit,
    private val onLongClick: (PoiModel) -> Unit
) : RecyclerView.Adapter<PoiAdapter.PoiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoiViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poi, parent, false)
        return PoiViewHolder(v)
    }

    override fun onBindViewHolder(holder: PoiViewHolder, position: Int) {
        holder.bind(pois[position])
    }

    override fun getItemCount(): Int = pois.size

    inner class PoiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTv: TextView = itemView.findViewById(R.id.textPoiName)
        private val commsTv: TextView = itemView.findViewById(R.id.textPoiComments)

        fun bind(poi: PoiModel) {
            nameTv.text = poi.name
            commsTv.text = poi.commentaires
            itemView.setOnClickListener { onClick(poi) }
            itemView.setOnLongClickListener {
                onLongClick(poi)
                true
            }
        }
    }
}

