package com.example.app.helpers

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.app.managers.JourneyManager
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.app.models.PhotoModel


class PhotoHelper(private val context: Context) {

    private var photoUri: Uri? = null
    lateinit var onPhotoReady: (Uri) -> Unit  // Callback quand la photo est prête

    // Créer un fichier temporaire pour la photo
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    // Lance la caméra pour prendre une photo
    fun takePhoto(photoResultLauncher: ActivityResultLauncher<Intent>) {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri) // → Dire où stocker l'image
        photoResultLauncher.launch(intent)
    }

    // Lance la galerie pour choisir une image
    fun pickFromGallery(galleryResultLauncher: ActivityResultLauncher<String>) {
        galleryResultLauncher.launch("image/*")
    }

    // Appeler cette fonction manuellement après avoir pris une photo
    fun onPhotoCaptured() {
        photoUri?.let {
            onPhotoReady(it)
        }
    }

    fun onPhotoReady(uri: Uri, location: GeoPoint?) {
        val dateString = getExifDateString(uri)
        val photo = PhotoModel(uri, location, date = dateString)
        JourneyManager.currentItinerary?.it_photos?.add(photo)
        attachPhotoToPoiIfPossible(photo)
    }




    fun getLocationFromImage(context: Context, uri: Uri): Location? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)

                val latLong = exif.latLong
                if (latLong != null) {
                    val location = Location("").apply {
                        latitude = latLong[0]
                        longitude = latLong[1]
                    }
                    location
                } else {
                    null // Pas de position trouvée
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun attachPhotoToPoiIfPossible(photo: PhotoModel) {
        val itinerary = JourneyManager.currentItinerary ?: return
        val pois = itinerary.interst_points

        photo.position?.let { photoPosition ->
            val nearestPoi = pois.minByOrNull { poi ->
                photoPosition.distanceToAsDouble(poi.location)
            }

            // Si un POI est proche (< 100 mètres)
            if (nearestPoi != null && photoPosition.distanceToAsDouble(nearestPoi.location) < 100.0) {
                photo.attachedPoiName = nearestPoi.name
            }
        }
    }

    private fun getExifDateString(uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return "?"
            val exif = ExifInterface(inputStream)
            val exifDate = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?: return "?"

            val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val date = parser.parse(exifDate)
            formatter.format(date!!)
        } catch (e: Exception) {
            e.printStackTrace()
            "?"
        }
    }


}
