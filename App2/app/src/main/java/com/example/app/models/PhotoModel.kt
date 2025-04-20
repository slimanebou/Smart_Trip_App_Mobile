package com.example.app.models

import android.net.Uri
import org.osmdroid.util.GeoPoint

data class PhotoModel(
    val uri: Uri? = null,                           // Uri locale de la photo
    val position: GeoPoint? = null,         // Position GPS
    var attachedPoiName: String? = null,    // Commentaire ou nom du POI
    var urlFirebase: String = ""            // URL Firebase après upload
)