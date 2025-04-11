package com.example.app.managers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.app.models.itinerary
import com.example.app.service.GpsTrackingService
import java.time.LocalDate

object JourneyManager {

    var currentItinerary: itinerary? = null

    fun startJourney(context: Context, ville: String?, date: LocalDate, name: String?) {
        Toast.makeText(context, "Voyage démarré !", Toast.LENGTH_SHORT).show()
        currentItinerary = itinerary(name, date, null, ville)

        val intent = Intent(context, GpsTrackingService::class.java)
        context.startService(intent)
    }

    fun stopJourney(context: Context) {
        val intent = Intent(context, GpsTrackingService::class.java)
        context.stopService(intent)
    }
}