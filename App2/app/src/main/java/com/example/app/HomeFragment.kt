package com.example.app

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.app.MainActivity.Companion.recording
import com.example.app.databinding.FragmentHomeBinding
import com.example.app.utils.MapConfigurator
import com.example.app.helpers.PermissionHelper
import com.example.app.helpers.PhotoHelper
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File
import com.bumptech.glide.Glide


class HomeFragment : Fragment() {

    private lateinit var imageViewProfileHome: ImageView

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
    private lateinit var photoHelper: PhotoHelper
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickPhotoLauncher: ActivityResultLauncher<String>
    private var tempPhotoUri: Uri? = null



    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation des launchers

        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                tempPhotoUri?.let { uri ->
                    if (PermissionHelper.hasLocationPermission(requireContext())) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                val point = GeoPoint(location.latitude, location.longitude)
                                photoHelper.onPhotoReady(uri, point)
                            } else {
                                photoHelper.onPhotoReady(uri, null) // Pas de GPS trouvé
                            }
                        }
                    }
                }
            }
        }


        pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { selectedImageUri ->
                val location = photoHelper.getLocationFromImage(requireContext(), selectedImageUri) // ← récupère une Location?

                val geoPoint = location?.let {
                    GeoPoint(it.latitude, it.longitude)
                } // ← transforme en GeoPoint

                photoHelper.onPhotoReady(selectedImageUri, geoPoint)
            }
        }


    }


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
        imageViewProfileHome = view.findViewById(R.id.imageViewProfileHome)

        mapView = view.findViewById(R.id.mapView)
        gpsDeniedMessage = view.findViewById(R.id.gpsDeniedMessage)
        blurOverlay = view.findViewById(R.id.blurOverlay)
        pauseButton = view.findViewById(R.id.pauseButton)
        resumeButton = view.findViewById(R.id.resumeButton)
        val addPhotoButton = view.findViewById<FloatingActionButton>(R.id.addPhotoButton)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        photoHelper = PhotoHelper(requireContext())


        // Configurer la carte
        MapConfigurator.initializeOSMDroid(requireContext())
        MapConfigurator.configureMap(mapView)

        recording.observe(viewLifecycleOwner) { isRecording ->
            setupButtons(isRecording)
            addPhotoButton.visibility = if (isRecording) View.VISIBLE else View.GONE

        }


        val shouldStartJourney = arguments?.getBoolean("startJourney", false) == true
        val nom = arguments?.getString("nom") ?: ""
        val ville = arguments?.getString("ville") ?: ""
        val isPublic = arguments?.getBoolean("isPublic") ?: false
        val countryCode = arguments?.getString("countryCode") ?: ""

        if (PermissionHelper.hasLocationPermission(requireContext())) {
            showMap()
            loadCurrentPosition()
            if (shouldStartJourney) {
                startJourney(ville, nom, isPublic, countryCode)
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

        // ?.let{} S'exécute SEULEMENT si currentUserUid n'est pas null
        currentUserUid?.let { user ->
            val uid = user.uid // Récupère l'UID

            usersRef.child(uid).child("profilePhotoUrl").get()
                .addOnSuccessListener { snapshot ->
                    if (!isAdded || context == null || view == null || _binding == null) return@addOnSuccessListener

                    val url = snapshot.value?.toString()
                    if (!url.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(url)
                            .into(imageViewProfileHome)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Impossible de charger la photo de profil", Toast.LENGTH_SHORT).show()
                }


            // recuperer et afficher les données d'utilisateur spécifique
            usersRef.child(uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Récupération directe du champ
                    val firstName = snapshot.child("firstName").value?.toString()
                        binding.textView2.text = "Hi, $firstName"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Gestion des erreurs
                    Log.e("Firebase", "Erreur de lecture des données : ${error.message}")
                }
            })
        }

        addPhotoButton.setOnClickListener {
            if (recording.value == true || JourneyManager.currentItinerary != null) {
                showPhotoChoiceDialog()
            } else {
                Toast.makeText(requireContext(), "Veuillez démarrer un trajet avant d'ajouter des photos.", Toast.LENGTH_SHORT).show()
            }
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


    private fun showPhotoChoiceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Ajouter une photo")
            .setItems(arrayOf("Prendre une photo", "Choisir depuis la galerie")) { _, which ->
                when (which) {
                    0 -> openCamera() // 👈 appelle la fonction ci-dessus
                    1 -> pickPhotoLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }




    @SuppressLint("MissingPermission")
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
    fun startJourney(ville: String?, name: String?, isPublic : Boolean, countryCode : String) {
        JourneyManager.startJourney(requireContext(), ville, java.time.LocalDate.now(), name,
            isPublic, countryCode)
    }

    fun stopJourney() {
        JourneyManager.stopJourney(requireContext())
        JourneyManager.currentItinerary?.let {
            MapManager.drawItinerary(it, mapView)
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

    private fun handlePhoto(uri: Uri) {
        // Ici tu peux afficher la photo, la stocker dans l'itinéraire, etc.
        Toast.makeText(requireContext(), "Photo ajoutée !", Toast.LENGTH_SHORT).show()

        // TODO : Associer cette photo au trajet ou à un POI si tu veux
    }

    private fun openCamera() {
        val context = requireContext()
        val photoFile = File.createTempFile(
            "photo_", ".jpg", context.cacheDir
        )
        tempPhotoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )

        tempPhotoUri?.let { safeUri ->
            takePhotoLauncher.launch(safeUri)
        }
    }



}
