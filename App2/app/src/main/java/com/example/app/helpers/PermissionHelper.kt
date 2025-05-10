package com.example.app.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.provider.Settings


object PermissionHelper {

    fun hasLocationPermission(context: Context): Boolean {
        /*
        Fonction pour vérifier si on possède les permissions
        */
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission(context: Any, requestCode: Int = 1) {
        /*
        Fonction pour vérifier pour redemander les permissions ou bien envoyer l'utilisateur aux
        paramètres
        */
        val appContext = when (context) {
            is Fragment -> context.requireContext()
            is Activity -> context
            else -> throw IllegalArgumentException("Context must be an Activity or Fragment")
        }

        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        // 1. Si la permission est déja accordé on ne fait rien
        if (ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED) return

        val showRationale = when (context) {
            is Fragment -> ActivityCompat.shouldShowRequestPermissionRationale(context.requireActivity(), permission)
            is Activity -> ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
            else -> false
        }

        // On vérifie si on peut redemander
        if (showRationale) {
            //  On peut redemander, nous affichons un message
            Toast.makeText(appContext, "Cette permission est nécessaire pour afficher la carte.", Toast.LENGTH_LONG).show()

            when (context) {
                is Fragment -> context.requestPermissions(arrayOf(permission), requestCode)
                is Activity -> ActivityCompat.requestPermissions(context, arrayOf(permission), requestCode)
            }
        } else {
            //  L'utilisateur a coché "Ne plus demander"
            Toast.makeText(appContext, "Veuillez activer la localisation dans les paramètres de l'application.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", appContext.packageName, null)
            }
            appContext.startActivity(intent)
        }
    }


    fun isGpsEnabled(context: Context): Boolean {
        // Fonction pour vérifier si le GPS est activé
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }



}