package com.example.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.app.FirebaseStorageHelper

class EditProfileFragment : Fragment() {

    private lateinit var imageProfile: ImageView
    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var saveButton: Button

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        imageProfile = view.findViewById(R.id.imageProfileEdit)
        firstNameInput = view.findViewById(R.id.editFirstName)
        lastNameInput = view.findViewById(R.id.editLastName)
        saveButton = view.findViewById(R.id.buttonSaveProfile)

        auth = FirebaseAuth.getInstance()
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        val uid = auth.currentUser?.uid

        uid?.let {
            usersRef.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").value?.toString() ?: ""
                    val lastName = snapshot.child("lastName").value?.toString() ?: ""
                    val photoUrl = snapshot.child("profilePhotoUrl").value?.toString()

                    firstNameInput.setText(firstName)
                    lastNameInput.setText(lastName)

                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(requireContext()).load(photoUrl).into(imageProfile)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erreur de chargement", Toast.LENGTH_SHORT).show()
                }
            })
        }

        imageProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            val firstName = firstNameInput.text.toString()
            val lastName = lastNameInput.text.toString()

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(requireContext(), "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uid?.let {
                usersRef.child(it).child("firstName").setValue(firstName)
                usersRef.child(it).child("lastName").setValue(lastName)
            }

            selectedImageUri?.let { uri ->
                FirebaseStorageHelper.uploadProfilePhoto(uri,
                    onSuccess = { downloadUrl ->
                        uid?.let {
                            usersRef.child(it).child("profilePhotoUrl").setValue(downloadUrl)
                        }
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Erreur photo: ${it.message}", Toast.LENGTH_SHORT).show()
                    })
            }

            Toast.makeText(requireContext(), "Profil mis à jour", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
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
