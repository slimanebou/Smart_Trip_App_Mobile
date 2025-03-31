package com.example.app

import android.app.FragmentManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.app.databinding.ActivityMainBinding


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
    }

    // function to replace fragment in navigation bar
   private fun replaceFragment(fragment: Fragment){
       val fragmentManager = supportFragmentManager
       val fragmentTransaction = fragmentManager.beginTransaction()
       fragmentTransaction.replace(R.id.frame_layout, fragment)
       fragmentTransaction.commit()
   }

}

