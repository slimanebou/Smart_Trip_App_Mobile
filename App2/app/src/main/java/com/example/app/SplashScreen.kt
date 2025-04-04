package com.example.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {

    private lateinit var fireAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Initialize Firebase Auth
        fireAuth = FirebaseAuth.getInstance()


        // fonction qui Exécute le code après un délai spécifié
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, StartActivity::class.java))
            finish() // fermer l'activité actuelle pour empêcher de revenir en arrière
        }, 2000) // 2000 ms = 3 secondes
    }


    //stile connect if already logged in
    override fun onStart() {
        super.onStart()
        if (fireAuth.currentUser != null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}