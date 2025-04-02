package com.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.app.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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

        val fab = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        fab.setOnClickListener {
            val fragmentManager = supportFragmentManager
            val currentFragment = fragmentManager.findFragmentById(R.id.frame_layout)

            if (currentFragment is HomeFragment) {
                currentFragment.prepareToStartJourney()
            } else {
                val homeFragment = HomeFragment()
                val bundle = Bundle()
                bundle.putBoolean("shouldStartJourney", true)
                homeFragment.arguments = bundle

                // Appelle ta nouvelle fonction + force sélection "Home"
                replaceFragment(homeFragment, R.id.home)
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


}

