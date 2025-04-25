package com.example.app.models

import com.google.firebase.firestore.GeoPoint
import java.io.Serializable

data class PoiModel(
    var id: String = "",
    var name: String = "",
    var commentaires: String = "",
    var location: GeoPoint? = null,
    var photos: List<String> = emptyList(),
    var createdBy: String = ""
) : Serializable
