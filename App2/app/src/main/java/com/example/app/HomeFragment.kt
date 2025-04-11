package com.example.app

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.app.MainActivity.Companion.recording
import com.example.app.utils.MapConfigurator
import com.example.app.helpers.PermissionHelper
import com.example.app.managers.JourneyManager
import com.example.app.managers.MapManager
import com.example.app.service.GpsTrackingService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var gpsDeniedMessage: TextView
    private lateinit var blurOverlay: View
    private lateinit var pauseButton: Button
    private lateinit var resumeButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialisation
        mapView = view.findViewById(R.id.mapView)
        gpsDeniedMessage = view.findViewById(R.id.gpsDeniedMessage)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        pauseButton = view.findViewById(R.id.pauseButton)
        resumeButton = view.findViewById(R.id.resumeButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Configurer la carte
        MapConfigurator.initializeOSMDroid(requireContext())
        MapConfigurator.configureMap(mapView)

        recording.observe(viewLifecycleOwner) { isRecording ->
            setupButtons(isRecording)
        }


        val shouldStartJourney = arguments?.getBoolean("startJourney", false) == true
        val nom = arguments?.getString("nom") ?: ""
        val ville = arguments?.getString("ville") ?: ""

        if (PermissionHelper.hasLocationPermission(requireContext())) {
            showMap()
            loadCurrentPosition()
            if (shouldStartJourney) {
                startJourney(ville, nom)
            }
        } else {
            hideMap()
            PermissionHelper.requestLocationPermission(this)
        }
    }

    private fun setupButtons(isRecording: Boolean) {
        if (isRecording) {
            pauseButton.visibility = View.VISIBLE
            resumeButton.visibility = View.GONE

            pauseButton.setOnClickListener {
                GpsTrackingService.paused = true
                Toast.makeText(requireContext(), "Pause du trajet", Toast.LENGTH_SHORT).show()
                pauseButton.visibility = View.GONE
                resumeButton.visibility = View.VISIBLE
            }

            resumeButton.setOnClickListener {
                GpsTrackingService.paused = false
                Toast.makeText(requireContext(), "Reprise du trajet", Toast.LENGTH_SHORT).show()
                resumeButton.visibility = View.GONE
                pauseButton.visibility = View.VISIBLE
            }
        } else {
            //  Quand recording est faux → désactiver les boutons
            pauseButton.setOnClickListener(null)
            resumeButton.setOnClickListener(null)

            pauseButton.visibility = View.GONE
            resumeButton.visibility = View.GONE
        }
    }



    private fun loadCurrentPosition() {
        if (PermissionHelper.hasLocationPermission(requireContext())) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val point = GeoPoint(it.latitude, it.longitude)
                    mapView.post {
                        MapManager.markPosition(mapView, point)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startJourney(ville: String?, name: String?) {
        JourneyManager.startJourney(requireContext(), ville, java.time.LocalDate.now(), name)
    }

    fun stopJourney() {
        JourneyManager.stopJourney(requireContext())
        JourneyManager.currentItinerary?.let {
            MapManager.drawItinerary(mapView, it)
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
}
