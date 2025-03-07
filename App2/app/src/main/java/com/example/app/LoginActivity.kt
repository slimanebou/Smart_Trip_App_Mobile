package com.example.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)


        val btn = findViewById<TextView>(R.id.textViewSingup)
        btn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val btn2 = findViewById<TextView>(R.id.btnSingip)
        btn2.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val btn3 = findViewById<TextView>(R.id.forgotPasswrod)
        btn3.setOnClickListener {
            val intent = Intent(this, forgotPassword::class.java)
            startActivity(intent)
        }
    }
}
