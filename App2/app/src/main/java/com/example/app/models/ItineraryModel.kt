package com.example.app.models
import org.osmdroid.util.GeoPoint
import java.time.LocalDate


class itinerary (name : String?, date_debut:LocalDate?, date_fin : LocalDate?,
                 ville_depart:String?, val it_points : MutableList<GeoPoint> = mutableListOf(),
                 private val interst_points : MutableList<GeoPoint> = mutableListOf()) {


    fun ajouterPointInteret(point: GeoPoint) {
        this.interst_points.add(point)
    }

    fun ajouterPointItineraire(point : GeoPoint) {
      this.it_points.add(point)
    }


}