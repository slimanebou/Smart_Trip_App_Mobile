package com.example.app.helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request


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

    suspend fun getCityAndCountryCode(lat: Double, lon: Double): Pair<String?, String?>? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "com.example.app/1.0")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val address = jsonObject.optJSONObject("address")
                    val city = address?.optString("city") ?: address?.optString("town") ?: address?.optString("village")
                    val countryCode = address?.optString("country_code")?.uppercase()
                    return@withContext city to countryCode
                }
            } catch (e: Exception) {
                Log.e("GeoHelper", "Erreur getCityAndCountryCode", e)
            }
            null
        }
    }

    suspend fun searchCityCoordinates(context: Context, cityName: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            val url = "https://nominatim.openstreetmap.org/search?q=${URLEncoder.encode(cityName, "UTF-8")}&format=json&limit=1"
            val request = Request.Builder().url(url).header("User-Agent", "SmartTripApp").build()
            val client = OkHttpClient()

            try {
                val response = client.newCall(request).execute()
                val json = JSONArray(response.body?.string())
                if (json.length() > 0) {
                    val obj = json.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    return@withContext lat to lon
                }
            } catch (e: Exception) {
                Log.e("GeoHelper", "Erreur de géocodage", e)
            }

            return@withContext null
        }
    }


}
