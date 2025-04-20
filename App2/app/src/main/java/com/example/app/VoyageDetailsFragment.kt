package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.app.managers.MapManager
import com.example.app.models.Itinerary
import com.example.app.models.Voyage
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import com.example.app.models.toItinerary


class VoyageDetailsFragment : Fragment() {

    private lateinit var mapView: MapView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_voyage_details, container, false)

        mapView = view.findViewById(R.id.mapVoyage)

        // Important pour la carte
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osm_prefs", 0)
        )
        mapView.setMultiTouchControls(true)

        return view
    }

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            val voyage = arguments?.getSerializable("voyage") as? Voyage ?: return@post
            val itinerary = voyage.toItinerary()
            MapManager.drawItinerary(itinerary, mapView)
        }
    }

}
