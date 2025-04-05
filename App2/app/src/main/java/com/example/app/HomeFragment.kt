package com.example.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.lifecycle.lifecycleScope
import com.example.app.models.MapModel
import com.example.app.models.itinerary
import com.example.app.service.GpsTrackingService
import com.example.app.utils.MapConfigurator
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.overlay.Polyline
import java.time.LocalDate



class HomeFragment : Fragment() {

    private lateinit var mapView: MapView // Vue de la map
    private lateinit var fusedLocationClient: FusedLocationProviderClient // Dernière localisation
    private lateinit var gpsDeniedMessage: TextView // Message affiché si permission refusée
    private lateinit var blurOverlay: View // Une vue flou de la map
    private var shouldStartJourney = false

    companion object {
        var itinerary: itinerary? = null
    }

    private lateinit var mapModel: MapModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MapConfigurator.initializeOSMDroid(requireContext())

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        return root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialisation des vues
        mapView = view.findViewById(R.id.mapView)
        gpsDeniedMessage = view.findViewById(R.id.gpsDeniedMessage)
        blurOverlay = view.findViewById(R.id.blurOverlay)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())


        mapModel = MapModel()
        MapConfigurator.configureMap(mapView)

        //  Correction : attendre que la vue soit affichée
            shouldStartJourney = arguments?.getBoolean("shouldStartJourney", false) == true
            val ville = arguments?.getString("villeDepart")
            val nomTrajet = arguments?.getString("nomTrajet")

            if (checkLocationPermission()) {
                showMap()
                if (shouldStartJourney) {
                    //  Ce n'est que ICI qu'on commence tout
                    shouldStartJourney = false
                    startJourney(ville, LocalDate.now(), nomTrajet)
                } else {
                    // Si pas de trajet à commencer, alors seulement marquer la position
                    getCurrentLocation { geoPoint ->
                        geoPoint?.let { markPosition(it, "Vous êtes ici") }
                    }
                }
            } else {
                hideMap()
                requestLocationPermission()
            }
    }



    private fun checkLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        return ContextCompat.checkSelfPermission(
            requireContext(), permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
    }

    private fun showMap() {
        blurOverlay.visibility = View.GONE
        gpsDeniedMessage.visibility = View.GONE
    }

    private fun hideMap() {
        blurOverlay.visibility = View.VISIBLE
        gpsDeniedMessage.visibility = View.VISIBLE
    }


    private fun markPosition(position:GeoPoint?, message:String) {
                    if (position != null && ::mapView.isInitialized) {
                        val point = GeoPoint(position.latitude, position.longitude)
                        mapView.controller.setCenter(point)

                        val marker = Marker(mapView)
                        marker.position = point
                        marker.title = message
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(marker)
                        mapView.invalidate()
                    }
    }


    fun getCurrentLocation(callback: (GeoPoint?) -> Unit) {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    callback(point)  //  Renvoie juste la position
                } else {
                    callback(null)   // Si la localisation n'est pas trouvée
                }
            }
        }
    }



    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            val permission = Manifest.permission.ACCESS_FINE_LOCATION

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission acceptée
                showMap()
                getCurrentLocation { geoPoint ->
                    if (geoPoint != null) {
                        markPosition(geoPoint, "Vous êtes ici")
                    }
                }

            } else {
                // Permission refusée
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)

                if (showRationale) {
                    //  L'utilisateur a juste refusé, on peut redemander
                    Toast.makeText(requireContext(), "La permission est nécessaire pour afficher la carte.", Toast.LENGTH_LONG).show()
                    requestLocationPermission() // 🔥 Redemander directement ou après un message
                } else {
                    //  L'utilisateur a coché "Ne plus demander"
                    Toast.makeText(requireContext(), "Veuillez autoriser la localisation dans les paramètres de l'application.", Toast.LENGTH_LONG).show()

                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                    startActivity(intent)
                }
                hideMap()
            }
        }
    }


    fun startJourney(nomVille : String?, dateD : LocalDate, name : String?) {
        Toast.makeText(requireContext(), "Voyage démarré !", Toast.LENGTH_SHORT).show()
        itinerary = itinerary(name, dateD, null, nomVille)
        getCurrentLocation { geoPoint ->
            geoPoint?.let { mapView.controller.setCenter(it) }
        }
        val intent = Intent(requireContext(), GpsTrackingService::class.java)
        requireContext().startService(intent)
    }

    fun stopJourney() {
        context?.let { ctx ->
            val intent = Intent(ctx, GpsTrackingService::class.java)
            ctx.stopService(intent)
            drawItinerary()
        }

    }

    private fun drawItinerary() {
        if (itinerary?.it_points.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Aucun trajet à afficher", Toast.LENGTH_SHORT).show()
            return
        }

        val polyline = Polyline()
        polyline.setPoints(itinerary!!.it_points)

        // Facultatif : tu peux personnaliser l'apparence de la ligne
        polyline.width = 5f
        polyline.color = android.graphics.Color.BLUE

        mapView.overlays.add(polyline)
        mapView.invalidate()

        val boundingBox = BoundingBox.fromGeoPoints(itinerary!!.it_points)
        mapView.zoomToBoundingBox(boundingBox, true, 50)
    }

}
