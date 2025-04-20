package com.example.app.models

import java.io.Serializable

data class PhotoMeta(
    val url: String = "",
    val date: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val commentaire: String = "",
    val utilisateur: String = ""
) : Serializable
