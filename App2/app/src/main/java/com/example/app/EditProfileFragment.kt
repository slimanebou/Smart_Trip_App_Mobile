package com.example.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileFragment : Fragment() {

    private lateinit var imageProfile: ImageView
    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_edit_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        imageProfile = view.findViewById(R.id.imageProfileEdit)
        firstNameInput = view.findViewById(R.id.editFirstName)
        lastNameInput = view.findViewById(R.id.editLastName)
        saveButton = view.findViewById(R.id.buttonSaveProfile)
        deleteButton = view.findViewById(R.id.buttonDeleteAccount)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("Utilisateurs").document(uid).get()
            .addOnSuccessListener { doc ->
                firstNameInput.setText(doc.getString("firstName") ?: "")
                lastNameInput.setText(doc.getString("lastName") ?: "")
                val photoUrl = doc.getString("profilePhotoUrl")
                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(requireContext()).load(photoUrl).into(imageProfile)
                }
            }

        imageProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            val firstName = firstNameInput.text.toString().trim()
            val lastName  = lastNameInput.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Tous les champs sont obligatoires",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid    = auth.currentUser!!.uid
            val fsRef  = firestore.collection("Utilisateurs").document(uid)
            val rtRef  = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)

            // 1) Prépare les mêmes données pour Firestore et Realtime DB
            val updates = mapOf(
                "firstName" to firstName,
                "lastName"  to lastName
            )

            // 2) Mets à jour Firestore
            fsRef.update(updates)
                .addOnCompleteListener {
                    // (optionnel) log ou Toast
                }

            // 3) Mets à jour Realtime DB
            rtRef.updateChildren(updates)
                .addOnCompleteListener {
                    // (optionnel) log ou Toast
                }

            // 4) Si une nouvelle photo est sélectionnée, on l’upload puis on propage l’URL
            selectedImageUri?.let { uri ->
                FirebaseStorageHelper.uploadProfilePhoto(uri,
                    onSuccess = { downloadUrl ->
                        // a) Firestore
                        fsRef.update("profilePhotoUrl", downloadUrl)
                        // b) Realtime DB
                        rtRef.child("profilePhotoUrl").setValue(downloadUrl)

                        // puis on propage aux voyages
                        propagateNameToTrips(uid, firstName, lastName, downloadUrl)
                    },
                    onFailure = {
                        Toast.makeText(requireContext(),
                            "Erreur upload photo: ${it.message}",
                            Toast.LENGTH_SHORT).show()
                        propagateNameToTrips(uid, firstName, lastName, null)
                    }
                )
            }

            // 5) Si pas de nouvelle photo, on propage directement le nom/prénom
            if (selectedImageUri == null) {
                propagateNameToTrips(uid, firstName, lastName, null)
            }

            Toast.makeText(requireContext(),
                "Profil mis à jour",
                Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    performAccountDeletion()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /**
     * Met à jour en batch tous les documents "voyages" de l’utilisateur
     * pour que ownerFirstName/LastName/(et photoUrl si non-null) soient à jour.
     */
    private fun propagateNameToTrips(
        uid: String,
        firstName: String,
        lastName: String,
        photoUrl: String?
    ) {
        val tripsRef = firestore
            .collection("Utilisateurs")
            .document(uid)
            .collection("voyages")

        tripsRef.get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                // Prépare la map des champs à mettre à jour
                val updates = mutableMapOf<String, Any>(
                    "ownerFirstName" to firstName,
                    "ownerLastName"  to lastName
                )
                photoUrl?.let { updates["ownerPhotoUrl"] = it }

                // Pour chaque voyage, on ajoute un update() au batch
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, updates)
                }
                // Commit du batch
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("EditProfile", "🚀 Tous les voyages ont été mis à jour")
                    }
                    .addOnFailureListener {
                        Log.e("EditProfile", "❌ Erreur batch update voyages", it)
                    }
            }
            .addOnFailureListener {
                Log.e("EditProfile", "❌ Impossible de lister les voyages", it)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            selectedImageUri = data.data
            imageProfile.setImageURI(selectedImageUri)
        }
    }

    private fun performAccountDeletion() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        // 1) Supprimer le doc Firestore
        firestore.collection("Utilisateurs")
            .document(uid)
            .delete()
            .addOnSuccessListener {
                // 2) Supprimer l’utilisateur Firebase Auth
                user.delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),
                            "Account deleted", Toast.LENGTH_SHORT).show()
                        // 3) Rediriger vers l’écran de login (ou splash)
                        requireActivity().finish()
                        startActivity(
                            Intent(requireContext(), LoginActivity::class.java)
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(),
                            "Auth deletion failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Firestore deletion failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
