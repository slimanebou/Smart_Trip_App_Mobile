package com.example.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.app.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    // Déclaration un variable mutable binding pour FragmentProfileBinding pour gerer les cycles de vie
    // _ c'est une convention Kotlin pour indique la version brute (comme * en rust)
    private var _binding: FragmentProfileBinding? = null

    private var valueListener: ValueEventListener? = null

    // Déclaration une variable immutable (propriété en lecture seule) binding pour FragmentProfileBinding pour gerer les cycles de vie
    private val binding get() = _binding!!

    // Initialize Firebase Auth
    private lateinit var auth: FirebaseAuth

    // Initialize Firebase Realtime Database
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference // Référence spécifique à "users"

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialisation correcte du View Binding
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Détection du clic sur le bouton de my trips
        binding.myTripsLayout.setOnClickListener {
            animation(it)

            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, MyTripsFragment())
                .addToBackStack(null)
                .commit()
        }
        // Détection du clic sur le bouton de langue
        binding.languageLayout.setOnClickListener {
            animation(it)
        }

        // Détection du clic sur le bouton de my favorite
        binding.myFavoriteLayout.setOnClickListener {
            animation(it)
            val intent = Intent(requireActivity(), FavoriteFragment::class.java)
        }

        // Détection du clic sur le bouton de update profil
        binding.settingsLayout.setOnClickListener {
            animation(it)

            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        // Détection du clic sur le bouton de déconnexion
        binding.logoutLayout.setOnClickListener {
            animation(it)
            showLogoutConfirmationDialog()
        }



        auth = FirebaseAuth.getInstance()

        // referece database firebase for writing or reading data
        database = FirebaseDatabase.getInstance()

        // Pointe vers users
        usersRef = database.getReference("users")

        // Récupère l'UID de l'utilisateur connecté
        val currentUserUid = FirebaseAuth.getInstance().currentUser

        // ?.let{} S'exécute SEULEMENT si currentUserUid n'est pas null
        currentUserUid?.let { user ->
            val uid = user.uid // Récupère l'UID

            //  Chargement de la photo de profil
            usersRef.child(uid).child("profilePhotoUrl").get()
                .addOnSuccessListener { snapshot ->
                    val url = snapshot.value?.toString()
                    if (!url.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(url)
                            .into(binding.imageViewProfile)
                    }
                }


            valueListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || view == null || _binding == null) return

                    val firstName = snapshot.child("firstName").value?.toString()
                    val lastName = snapshot.child("lastName").value?.toString()
                    val email = snapshot.child("email").value?.toString()
                    binding.textView6.text = "$firstName $lastName"
                    binding.textView7.text = email
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            usersRef.child(uid).addValueEventListener(valueListener!!)


        }


            // Clic pour choisir une nouvelle photo
        binding.imageViewProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            selectedImageUri = data.data

            selectedImageUri?.let { uri ->
                FirebaseStorageHelper.uploadProfilePhoto(
                    imageUri = uri,
                    onSuccess = { downloadUrl ->
                        Glide.with(requireContext())
                            .load(downloadUrl)
                            .into(binding.imageViewProfile)
                        Toast.makeText(requireContext(), "Photo mise à jour !", Toast.LENGTH_SHORT).show()

                        // Mise à jour dans Realtime Database aussi (optionnel, si nécessaire)
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        uid?.let {
                            usersRef.child(it).child("profilePhotoUrl").setValue(downloadUrl)
                        }
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Erreur : ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun animation(it : View) {
        it.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                it.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
            }
    }

    // Création d'un AlertDialog avec un style
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")

            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss() // Fermer le dialogue
                logout() // Déconnexion
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Fermer le dialogue
            }

            // donner l'accessibilité à l'utilisateur pour fermer le dialogue sans cliquer sur un bouton annuler (en cliquant a l'extérieur)
            .setCancelable(true)
            .create()
            .show()
    }

    // fonction de déconnexion
    private fun logout() {
        auth.signOut()

        // Redirection vers LoginActivity
        val intent = Intent(requireActivity(), LoginActivity::class.java)

        // Nettoyer l'historique de navigation
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Fermer l'activité courante
        requireActivity().finish()
    }

    // pour éviter les fuites de mémoire (backing property pattern)
    override fun onDestroyView() {
        super.onDestroyView()
        valueListener?.let {
            usersRef.removeEventListener(it)
        }
        _binding = null
    }





}