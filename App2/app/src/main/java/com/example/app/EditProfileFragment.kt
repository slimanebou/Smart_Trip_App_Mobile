package com.example.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileFragment : Fragment() {

    private lateinit var imageProfile: ImageView
    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var saveButton: Button

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
            val lastName = lastNameInput.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(requireContext(), "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid    = auth.currentUser!!.uid
            val userRef = firestore.collection("Utilisateurs").document(uid)

            userRef.update(
                mapOf(
                    "firstName"      to firstName,
                    "lastName"       to lastName
                )
            ).addOnSuccessListener {
                // 2) S’il n’y a pas de nouvelle photo, on peut directement propager le nom/prénom
                propagateNameToTrips(uid, firstName, lastName, null)
            }

            // 3) Si une nouvelle photo a été sélectionnée, on l’upload puis on propage aussi l’URL
            selectedImageUri?.let { uri ->
                FirebaseStorageHelper.uploadProfilePhoto(uri,
                    onSuccess = { downloadUrl ->
                        userRef.update("profilePhotoUrl", downloadUrl)
                            .addOnSuccessListener {
                                propagateNameToTrips(uid, firstName, lastName, downloadUrl)
                            }
                    },
                    onFailure = {
                        Toast.makeText(requireContext(),
                            "Photo upload error: ${it.message}", Toast.LENGTH_SHORT).show()
                        // on propage quand même le nom/prénom
                        propagateNameToTrips(uid, firstName, lastName, null)
                    })
            }

            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
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
}
