package com.example.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.app.databinding.ActivityRegisterBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var fireAuth: FirebaseAuth

    // Initialize Firebase Realtime Database
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference // Référence spécifique à "users"

    private lateinit var imageUri : Uri

    private var isShowPassword = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Initialize Firebase Storage reference
        var myRef = FirebaseStorage.getInstance().reference

        // referece database firebase for writing or reading data
        database = FirebaseDatabase.getInstance()

        // Pointe vers users
        usersRef = database.getReference("users")


        // Initialize Firebase Auth
        fireAuth = FirebaseAuth.getInstance()

        //return to login
        binding.alreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Fonction pour afficher ou masquer le password
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
            val firstName = binding.inputFirstName.text.toString()
            val lastName = binding.inputLastName.text.toString()
            val email = binding.inputEmailR.text.toString()
            val pass = binding.inputPasswordR.text.toString()
            val confirmpass = binding.inputConfirmpasswordR.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmpass.isNotEmpty()) {
                if (pass == confirmpass) {
                    fireAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                // Récupération du user APRÈS création réussie
                                val firebaseUser = fireAuth.currentUser
                                firebaseUser?.let { user ->
                                    saveUserData(
                                        uid = user.uid,
                                        firstName = firstName,
                                        lastName = lastName,
                                        email = email
                                    )

                                    Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Error: ${authTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Password mismatch", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty fields not allowed", Toast.LENGTH_SHORT).show()
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


    private fun saveUserData(uid: String, firstName: String, lastName: String, email: String) {
        val user = User(firstName, lastName, email)
        usersRef.child(uid).setValue(user)
            .addOnSuccessListener {
                Log.d("Firebase", "Data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Save failed", e)
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // ✅ Firestore
        val firestore = FirebaseFirestore.getInstance()
        val userMap = hashMapOf(
            "name" to "$firstName $lastName",
            "email" to email,
            "profilePicture" to null, // Tu peux le mettre à jour plus tard si tu ajoutes une photo
            "settings" to mapOf(
                "batterySaver" to true,
                "gpsInterval" to 5
            )
        )

        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Log.d("Firestore", "User added to Firestore!")
                Toast.makeText(this, "Firestore: User saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to save user to Firestore", e)
                Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        Log.d("RegisterActivity", "Trying to save Firestore user with uid=$uid")

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