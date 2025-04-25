package com.example.app

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.app.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.app.helpers.PermissionHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    companion object{ var recording = MutableLiveData(false) }
    private lateinit var fab : FloatingActionButton
    private var isPublicTrip = false


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set home fragment as default fragment (start)
        replaceFragment(HomeFragment(), R.id.home)

        // transition between fragments in navigation bar
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.journey -> replaceFragment(JourneyFragment())
                R.id.profile -> replaceFragment(ProfileFragment())
                R.id.favorite -> replaceFragment(FavoriteFragment())
                else -> {
                }
            }
            true
        }

        fab = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        fab.setOnClickListener {
            if (PermissionHelper.hasLocationPermission(this)) {
                if (!recording.value!!) {
                    if (PermissionHelper.isGpsEnabled(this)) {
                        showStartJourneyDialog()
                    } else {
                        Toast.makeText(
                            this, "Veuillez activer votre localisation GPS.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    showStopJourneyConfirmation()
                }
            } else {
                Toast.makeText(
                    this, "Veuillez accorder les permission" +
                            " la localisation et activer le gps.", Toast.LENGTH_SHORT
                ).show()
                PermissionHelper.requestLocationPermission(this)
            }
        }


    }





    private fun replaceFragment(fragment: Fragment, selectNavItemId: Int? = null) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()

        // Mettre à jour l'état du bouton de navigation si un ID est fourni
        selectNavItemId?.let {
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNav.selectedItemId = it
        }
    }

    @SuppressLint("CutPasteId", "MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    fun showStartJourneyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_start_journey, null)

        val villeDepartInput = dialogView.findViewById<EditText>(R.id.villeDepartInput)
        val nomTrajetInput = dialogView.findViewById<EditText>(R.id.nomTrajetInput)
        val trajetPublicCheckBox = dialogView.findViewById<CheckBox>(R.id.trajetPublicCheckBox)

        // 🔁 Détection automatique de la ville + pays
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        if (PermissionHelper.hasLocationPermission(this)) {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lifecycleScope.launch {
                        val result = com.example.app.helpers.GeoHelper.getCityAndCountryCode(
                            location.latitude,
                            location.longitude
                        )
                        val villeAuto = result?.first
                        val codePays = result?.second

                        villeDepartInput.setText(villeAuto ?: "")
                        villeDepartInput.tag = codePays //  Stocke le countryCode dans le champ invisible
                    }
                }
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Démarrer") { dialog, id ->
                val nomTrajet = nomTrajetInput.text.toString()
                val villeDepart = villeDepartInput.text.toString()
                val countryCode = villeDepartInput.tag as String // Récupère ce qu'on a mis plus haut
                isPublicTrip = trajetPublicCheckBox.isChecked

                fab.setImageResource(R.drawable.close)
                navigateToHomeAndStartJourney(nomTrajet, villeDepart, isPublicTrip, countryCode)
            }
            .setNegativeButton("Annuler") { dialog, id ->
                dialog.dismiss()
            }

        builder.create().show()
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun navigateToHomeAndStartJourney(nom: String, ville: String, isPublic : Boolean,
                                              countryCode : String) {

        val homeFragment = HomeFragment()
        recording.value = true
        //  Passer les données au fragment
        val bundle = Bundle()
        bundle.putBoolean("startJourney", true) // Démarrer le voyage
        bundle.putString("nom", nom)
        bundle.putString("ville", ville)
        bundle.putBoolean("isPublic", isPublic)
        bundle.putString("countryCode", countryCode)
        homeFragment.arguments = bundle

        //  Remplacer par HomeFragment avec les infos
        replaceFragment(homeFragment, R.id.home)

    }


    private fun showStopJourneyConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Terminer le trajet ?")
            .setMessage("Voulez-vous vraiment terminer votre trajet ?")
            .setPositiveButton("Oui") { dialog, _ ->
                recording.value = false
                val fragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
                if (fragment is HomeFragment && fragment.isAdded) {
                    //  HomeFragment est prêt → appel direct
                    fragment.stopJourney()
                } else {
                    //  HomeFragment pas prêt → on attend qu'il soit affiché
                    replaceFragment(HomeFragment(), R.id.home)
                    supportFragmentManager.executePendingTransactions() //  Force le chargement immédiat
                    val newFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
                    if (newFragment is HomeFragment) {
                        newFragment.stopJourney()
                    }
                }

                fab.setImageResource(R.drawable.add)
                dialog.dismiss()
            }
            .setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



}

