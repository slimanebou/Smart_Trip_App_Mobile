package com.example.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.app.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {

    // Déclaration un variable mutable binding pour FragmentProfileBinding pour gerer les cycles de vie
    // _ c'est une convention Kotlin pour indique la version brute (comme * en rust)
    // ? c'est une convention Kotlin pour indique que la variable peut être null
    private var _binding: FragmentProfileBinding? = null

    // Déclaration une variable immutable (propriété en lecture seule) binding pour FragmentProfileBinding pour gerer les cycles de vie
    // !! c'est une convention Kotlin pour indique que la variable ne peut pas être null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

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

        auth = FirebaseAuth.getInstance()

        binding.logoutLayout.setOnClickListener {
            showLogoutConfirmationDialog()
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

            // donner l'accessibilité à l'utilisateur pour fermer le dialogue sans cliquer sur un bouton (en cliquant a l'extérieur)
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
        // Clear the binding reference to prevent memory leaks
        // as the Fragment's view hierarchy is destroyed
        _binding = null
    }


}