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
import com.example.app.databinding.FragmentHomeBinding
import com.example.app.utils.MapConfigurator
import com.example.app.helpers.PermissionHelper
import com.example.app.managers.JourneyManager
import com.example.app.managers.MapManager
import com.example.app.service.GpsTrackingService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class HomeFragment : Fragment() {

    // Déclaration un variable mutable binding pour FragmentProfileBinding pour gerer les cycles de vie
    // _ c'est une convention Kotlin pour indique la version brute (comme * en rust)
    private var _binding: FragmentHomeBinding? = null

    // Déclaration une variable immutable (propriété en lecture seule) binding pour FragmentProfileBinding pour gerer les cycles de vie
    private val binding get() = _binding!!


    // Initialize Firebase Realtime Database
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference // Référence spécifique à "users"


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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
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



        // referece database firebase for writing or reading data
        database = FirebaseDatabase.getInstance()

        // Pointe vers users
        usersRef = database.getReference("users")

        // Récupère l'UID de l'utilisateur connecté
        val currentUserUid = FirebaseAuth.getInstance().currentUser

        currentUserUid?.let { user ->
            val uid = user.uid

            // Écoute les données de l'utilisateur spécifique
            usersRef.child(uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Récupération directe du champ
                    val firstName = snapshot.child("firstName").value?.toString()
                        binding.textView2.text = "Hi, $firstName"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(),
                        "Erreur de lecture: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                }
            })
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
