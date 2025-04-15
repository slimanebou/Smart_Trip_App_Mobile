package com.example.app.models

import android.net.Uri
import org.osmdroid.util.GeoPoint

data class PhotoModel(
    val uri: Uri,
    val position: GeoPoint?,
    var attachedPoiName: String? = null // null si attaché au trajet seulement
)
