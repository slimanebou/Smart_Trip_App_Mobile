package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import com.example.app.managers.MapManager
import com.example.app.models.PointOfInterest
import com.example.app.models.Voyage
import com.example.app.models.toItinerary
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.util.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView

class VoyageDetailsFragment : Fragment() {

    private lateinit var mapView: MapView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // on utilise le layout qui contient maintenant un MapView + un FAB
        return inflater.inflate(R.layout.fragment_voyage_details, container, false)
    }

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialise la carte
        mapView = view.findViewById(R.id.mapVoyage)
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osm_prefs", 0)
        )
        mapView.setMultiTouchControls(true)

        // récupère l'objet Voyage passé en argument
        val voyage = arguments
            ?.getSerializable("voyage") as? Voyage
            ?: return

        // trace l'itinéraire
        // 1) On construit l’Itinerary initial (points + photos)
        val itinerary = voyage.toItinerary()

// 2) On récupère les POI depuis Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("Utilisateurs")
            .document(voyage.utilisateur)
            .collection("voyages")
            .document(voyage.id)
            .collection("poi")
            .get()
            .addOnSuccessListener { snap ->
                // 3) Pour chaque POI, on crée un objet PointOfInterest
                for (doc in snap.documents) {
                    val name = doc.getString("name").orEmpty()
                    val desc = doc.getString("description").orEmpty()
                    val lat  = doc.getDouble("latitude") ?: continue
                    val lon  = doc.getDouble("longitude") ?: continue

                    val poi = PointOfInterest(
                        name        = name,
                        description = desc,
                        location    = GeoPoint(lat, lon)
                    )
                    itinerary.ajouterPointInteret(poi)
                }
                // 4) Quand c’est prêt, on dessine tout
                view.post { MapManager.drawItinerary(itinerary, mapView) }
            }
            .addOnFailureListener {
                // En cas d’erreur on affiche au moins l’itinéraire sans POI
                view.post { MapManager.drawItinerary(itinerary, mapView) }
            }
        // bouton pour voir la liste des POIs
        view.findViewById<AppCompatImageButton>(R.id.btnViewPois)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.frame_layout,
                    PoiListFragment.newInstance(
                        voyageId = voyage.id,
                        ownerId  = voyage.utilisateur
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }
}
