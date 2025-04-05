package com.example.app

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.app.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDate
import java.util.logging.Logger.global


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var recording = false
    private lateinit var fab : FloatingActionButton


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set home fragment as default fragment (start)
        replaceFragment(HomeFragment())

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
            if (!recording) {
                showStartJourneyDialog()
            }
            else {
                showStopJourneyConfirmation()
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun showStartJourneyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_start_journey, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Démarrer") { dialog, id ->
                val nomTrajet =
                    dialogView.findViewById<android.widget.EditText>(R.id.nomTrajetInput).text.toString()
                val villeDepart =
                    dialogView.findViewById<android.widget.EditText>(R.id.villeDepartInput).text.toString()

                //  Quand il clique sur "Démarrer" :
                fab.setImageResource(R.drawable.close)
                navigateToHomeAndStartJourney(nomTrajet, villeDepart)
            }
            .setNegativeButton("Annuler") { dialog, id ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun navigateToHomeAndStartJourney(nom: String, ville: String) {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.frame_layout)
        recording = true
        if (currentFragment is HomeFragment) {
            // 🔥 Si déjà dans HomeFragment → utiliser une fonction spéciale
            currentFragment.startJourney(ville, LocalDate.now(), nom)
        } else {
            //  Sinon → juste changer de fragment avec les données
            val bundle = Bundle().apply {
                putBoolean("shouldStartJourney", true)
                putString("nomTrajet", nom)
                putString("villeDepart", ville)
            }
            val homeFragment = HomeFragment().apply {
                arguments = bundle
            }
            replaceFragment(homeFragment)
        }
    }


    private fun showStopJourneyConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Terminer le trajet ?")
            .setMessage("Voulez-vous vraiment terminer votre trajet ?")
            .setPositiveButton("Oui") { dialog, _ ->
                //  🔥 Trouver le HomeFragment actuellement affiché
                val fragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
                if (fragment is HomeFragment) {
                    fragment.stopJourney()
                }

                recording = false
                fab.setImageResource(R.drawable.add) //  Retourner au bouton +
                dialog.dismiss()
            }
            .setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


}

