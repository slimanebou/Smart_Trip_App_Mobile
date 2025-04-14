package com.example.app.helpers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeoHelper {

    suspend fun getPlaceName(lat: Double, lon: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "com.example.app/1.0") // obligatoire pour OSM
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    return@withContext jsonObject.optString("display_name", null)
                } else {
                    Log.e("GeoHelper", "Erreur HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("GeoHelper", "Erreur de récupération du lieu", e)
            }
            null
        }
    }
}
