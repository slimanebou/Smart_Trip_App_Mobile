package com.example.app

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.app.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var fireAuth: FirebaseAuth
    private var isShowPassword = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fireAuth = FirebaseAuth.getInstance()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //return to login
        binding.alreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }


        // Détecter le clic sur l'icône drawableRight[2] [0 left, 1 top, 2 right, 3 bottom]
        // _ = view ( (EditText) qui reçoit l'événement), event = un objet MotionEvent qui contient les informations sur l'événement (clic, déplacement, relachement, etc.)
        binding.inputPasswordR.setOnTouchListener { _, event ->
            // Vérifier si l'événement est un clic sur l'icône (l'utilisateur relâche son doigt après un clic.)
            if (event.action == MotionEvent.ACTION_UP) {

                // Vérifie l'endroit du click, si le clic est sur l'icône drawableRight
                if (event.rawX >= (binding.inputPasswordR.right - binding.inputPasswordR.compoundDrawables[2].bounds.width())) {
                    isShowPassword = !isShowPassword
                    showPassword(isShowPassword)
                }
            }
            false
        }

        binding.inputConfirmpasswordR.setOnTouchListener { _, event ->
            // Vérifier si l'événement est un clic sur l'icône (l'utilisateur relâche son doigt après un clic.)
            if (event.action == MotionEvent.ACTION_UP) {

                // Vérifie l'endroit du click, si le clic est sur l'icône drawableRight
                if (event.rawX >= (binding.inputConfirmpasswordR.right - binding.inputConfirmpasswordR.compoundDrawables[2].bounds.width())) {
                    isShowPassword = !isShowPassword
                    showPassword(isShowPassword)
                }
            }
            false
        }


        //Sing up
        binding.btnSingup.setOnClickListener {
            val email = binding.inputEmailR.text.toString()
            val pass = binding.inputPasswordR.text.toString()
            val confirmpass = binding.inputConfirmpasswordR.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmpass.isNotEmpty()){
                if (pass == confirmpass){
                    fireAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener{
                        if (it.isSuccessful){
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()

                        }else{
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }

                    }
                }else{
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }

        //return to login with back button
        binding.back.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // Transition de gauche a droit (nouvelle fon overrideActiviteTrasition marche uniquement sur android 12 et plus)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }

        // Handle the back button on phone with (< en bas)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })

    }

    // Fonction pour afficher ou masquer le password
    fun showPassword(isShown: Boolean) {
        binding.inputPasswordR.transformationMethod = if (isShown) {
            // Afficher le texte en clair
            android.text.method.HideReturnsTransformationMethod.getInstance()
        } else {
            // Masquer le texte
            android.text.method.PasswordTransformationMethod.getInstance()
        }

        binding.inputConfirmpasswordR.transformationMethod = if (isShown) {
            // Afficher le texte en clair
            android.text.method.HideReturnsTransformationMethod.getInstance()
        } else {
            // Masquer le texte
            android.text.method.PasswordTransformationMethod.getInstance()
        }

        // Déplacer le curseur à la fin du texte
        binding.inputPasswordR.setSelection(binding.inputPasswordR.text.length)
        binding.inputConfirmpasswordR.setSelection(binding.inputConfirmpasswordR.text.length)


        // Changer l'icône à droite dynamiquement
        val drawableEnd = if (isShown) R.drawable.visibiliy else R.drawable.visibility_off

        // ça adapte automatiquement la taille de l'image , Cela permet d’éviter les erreurs de mise en page
        binding.inputPasswordR.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.security, 0, drawableEnd, 0
        )

        binding.inputConfirmpasswordR.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.security, 0, drawableEnd, 0
        )
    }

}