package com.example.app.models
import org.osmdroid.util.GeoPoint
import java.time.LocalDate


class itinerary (name : String?, date_debut:LocalDate?, date_fin : LocalDate?,
                 ville_depart:String?, val it_points : MutableList<GeoPoint> = mutableListOf(),
                  val interst_points : MutableList<PointOfInterest> = mutableListOf(),
                    val it_photos: MutableList<PhotoModel> = mutableListOf()) {


    fun ajouterPointInteret(poi: PointOfInterest) {
        this.interst_points.add(poi)
    }

    fun ajouterPointItineraire(point : GeoPoint) {
      this.it_points.add(point)
    }


}