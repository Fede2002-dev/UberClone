package uberapp.balran.uberapp

import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.messaging.FirebaseMessaging
import uberapp.balran.uberapp.utilities.MyApp
class DriverHomeActivityViewModel:ViewModel() {
    fun suscribeTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("trip_request").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(MyApp.context, "Conectado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun unSuscribeTopic() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("trip_request").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(MyApp.context, "Desconectado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun distance(lat_a: Double, lng_a: Double, lat_b: Double, lng_b: Double): Float {
        val earthRadius = 3958.75
        val latDiff = Math.toRadians(lat_b - lat_a)
        val lngDiff = Math.toRadians(lng_b - lng_a)
        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = earthRadius * c
        val meterConversion = 1609
        return (distance * meterConversion.toFloat()).toFloat()
    }
}