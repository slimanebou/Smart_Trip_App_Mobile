package com.example.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: androidx.credentials.CredentialManager
    private lateinit var fireAuth: FirebaseAuth

    private var isShowPassword = false

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fireAuth = FirebaseAuth.getInstance()
        credentialManager = androidx.credentials.CredentialManager.create(this)

        binding.google.setOnClickListener { signInWithGoogle() }

        binding.textViewSingup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnSingip.setOnClickListener {
            val email = binding.inputEmail.text.toString()
            val pass = binding.inputPasswordS.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                fireAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = fireAuth.currentUser
                        user?.let {
                            saveUserIfNotExists(
                                uid = it.uid,
                                firstName = "",
                                lastName = "",
                                email = it.email ?: ""
                            )
                        }
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.forgotPasswrod.setOnClickListener {
            startActivity(Intent(this, ForgotPassword::class.java))
        }

        binding.inputPasswordS.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.inputPasswordS.right - binding.inputPasswordS.compoundDrawables[2].bounds.width())) {
                    isShowPassword = !isShowPassword
                    showPassword(isShowPassword)
                }
            }
            false
        }
    }

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
                Toast.makeText(this@LoginActivity, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                Toast.makeText(this, "Error processing Google sign-in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        fireAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = fireAuth.currentUser
                user?.let {
                    val email = it.email ?: return@let
                    val name = it.displayName ?: "Utilisateur"
                    val firstName = name.split(" ").firstOrNull() ?: ""
                    val lastName = name.split(" ").drop(1).joinToString(" ")
                    saveUserIfNotExists(it.uid, firstName, lastName, email)
                }

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserIfNotExists(uid: String, firstName: String, lastName: String, email: String) {
        val firestoreRef = FirebaseFirestore.getInstance().collection("Utilisateurs").document(uid)

        val userMap = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "profilePhotoUrl" to null,
            "settings" to mapOf(
                "batterySaver" to true,
                "gpsInterval" to 5
            )
        )

        firestoreRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                firestoreRef.set(userMap)
                    .addOnSuccessListener { Log.d(TAG, "✅ Utilisateur sauvegardé dans Firestore") }
                    .addOnFailureListener { Log.e(TAG, "❌ Échec Firestore", it) }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        if (fireAuth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showPassword(isShown: Boolean) {
        binding.inputPasswordS.transformationMethod = if (isShown)
            android.text.method.HideReturnsTransformationMethod.getInstance()
        else
            android.text.method.PasswordTransformationMethod.getInstance()

        binding.inputPasswordS.setSelection(binding.inputPasswordS.text.length)

        val drawableEnd = if (isShown) R.drawable.visibiliy else R.drawable.visibility_off
        binding.inputPasswordS.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.security, 0, drawableEnd, 0
        )
    }
}
