package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentJourneyBinding
import com.example.app.databinding.FragmentProfileBinding
import com.example.app.managers.VoyageAdapter
import com.example.app.models.Voyage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JourneyFragment : Fragment() {

    // Déclaration un variable mutable binding pour FragmentProfileBinding pour gerer les cycles de vie
    // _ c'est une convention Kotlin pour indique la version brute (comme * en rust)
    private var _binding: FragmentJourneyBinding? = null

    // Déclaration une variable immutable (propriété en lecture seule) binding pour FragmentProfileBinding pour gerer les cycles de vie
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJourneyBinding.inflate(inflater, container, false)
        return binding.root


    }
}