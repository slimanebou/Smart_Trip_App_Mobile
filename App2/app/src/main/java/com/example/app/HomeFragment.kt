package com.example.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView // Vue de la map
    private lateinit var fusedLocationClient: FusedLocationProviderClient // Dernière localisation
    private lateinit var gpsDeniedMessage: TextView // Message affiché si permission refusée
    private lateinit var blurOverlay: View // Une vue flou de la map
    private var permissionPreviouslyDenied = false
    private var shouldStartJourney = false



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialiser les vues ici (si ce n'est pas déjà fait)
        mapView = view.findViewById(R.id.mapView)
        gpsDeniedMessage = view.findViewById(R.id.gpsDeniedMessage)
        blurOverlay = view.findViewById(R.id.blurOverlay)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Configuration de la map
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Récupérer l'info depuis les arguments
        shouldStartJourney = arguments?.getBoolean("shouldStartJourney", false) == true
        checkLocationPermission()

    }


    private fun checkLocationPermission() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            showMap() // ✅ Affiche la carte (plus de flou)
            getCurrentLocation() // ✅ Centre la carte sur la position

            if (shouldStartJourney) {
                startJourney()
                shouldStartJourney = false
            }

        } else {
            hideMap() //  Cache la carte si permission refusée
            requestPermissions(arrayOf(permission), 1)
        }
    }




    private fun showMap() {
        blurOverlay.visibility = View.GONE
        gpsDeniedMessage.visibility = View.GONE
    }

    private fun hideMap() {
        blurOverlay.visibility = View.VISIBLE
        gpsDeniedMessage.visibility = View.VISIBLE
    }


    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val point = GeoPoint(location.latitude, location.longitude)
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(point)

                val marker = Marker(mapView)
                marker.position = point
                marker.title = "Vous êtes ici"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(marker)
                mapView.invalidate()
            }
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1 && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (shouldStartJourney) {
                    startJourney()
                    shouldStartJourney = false
                }
            } else {
                if (!permissionPreviouslyDenied) {
                    Toast.makeText(
                        requireContext(),
                        "Permission refusée. Activez le GPS pour démarrer le voyage.",
                        Toast.LENGTH_LONG
                    ).show()
                    blurOverlay.visibility = View.VISIBLE
                    gpsDeniedMessage.visibility = View.VISIBLE
                    permissionPreviouslyDenied = true
                }
            }
        }
    }



    fun startJourney() {
        Toast.makeText(requireContext(), "Voyage démarré !", Toast.LENGTH_SHORT).show()
        // Plus tard : démarrage du GPS tracking
    }

    fun prepareToStartJourney() {
        shouldStartJourney = true //  Active le "drapeau"
        checkLocationPermission()
    }

    /*override fun onResume() {
        super.onResume()
        checkLocationPermission()  //  vérifie les permissions
    }*/
}
