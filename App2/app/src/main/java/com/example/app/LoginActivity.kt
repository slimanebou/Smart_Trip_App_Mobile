package com.example.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.app.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: androidx.credentials.CredentialManager
    private lateinit var fireAuth: FirebaseAuth

    private var isShowPassword = false

    companion object {
        private const val TAG = "GoogleAuth"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        fireAuth = FirebaseAuth.getInstance()
        credentialManager = androidx.credentials.CredentialManager.create(this)


        // Set up Google Sign-In button click listener
        binding.google.setOnClickListener {
            signInWithGoogle()
        }

        //go to register activity
        binding.textViewSingup.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        //Sing in with email and password(user deja existe)
        binding.btnSingip.setOnClickListener {
            val email = binding.inputEmail.text.toString()
            val pass = binding.inputPasswordS.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()){
                fireAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener{
                    if (it.isSuccessful){
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                    }else{
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }

                }
            }else{
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            }

        }

        //go to forgot password activity
        binding.forgotPasswrod.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }


        // Détecter le clic sur l'icône drawableRight[2] [0 left, 1 top, 2 right, 3 bottom]
        // _ = view ( (EditText) qui reçoit l'événement), event = un objet MotionEvent qui contient les informations sur l'événement (clic, déplacement, relachement, etc.)
        binding.inputPasswordS.setOnTouchListener { _, event ->
            // Vérifier si l'événement est un clic sur l'icône (l'utilisateur relâche son doigt après un clic.)
            if (event.action == MotionEvent.ACTION_UP) {

                // Vérifie l'endroit du click, si le clic est sur l'icône drawableRight
                if (event.rawX >= (binding.inputPasswordS.right - binding.inputPasswordS.compoundDrawables[2].bounds.width())) {
                    isShowPassword = !isShowPassword
                    showPassword(isShowPassword)
                }
            }
            false
        }
    }


    //    --------------------------------------------------------------------------------------------------
    private fun signInWithGoogle() {
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity,
                )

                handleSignIn(response.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google sign-in failed", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Google sign-in failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Google ID token", e)
                Toast.makeText(this, "Error processing Google sign-in", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w(TAG, "Unexpected credential type: ${credential::class.java}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        fireAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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


    // Fonction pour afficher ou masquer le password
    fun showPassword(isShown: Boolean) {
        binding.inputPasswordS.transformationMethod = if (isShown) {
            // Afficher le texte en clair
            android.text.method.HideReturnsTransformationMethod.getInstance()
        } else {
            // Masquer le texte
            android.text.method.PasswordTransformationMethod.getInstance()
        }
        // Déplacer le curseur à la fin du texte
        binding.inputPasswordS.setSelection(binding.inputPasswordS.text.length)

        // Changer l'icône à droite dynamiquement
        val drawableEnd = if (isShown) R.drawable.visibiliy else R.drawable.visibility_off

        // ça adapte automatiquement la taille de l'image , Cela permet d’éviter les erreurs de mise en page
        binding.inputPasswordS.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.security, 0, drawableEnd, 0
        )

    }

}

















