package uberapp.balran.uberapp.clientHome

import android.app.Application
import androidx.lifecycle.ViewModel
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseUser
import org.json.JSONException
import org.json.JSONObject
import uberapp.balran.uberapp.utilities.MyApp
import java.util.HashMap

class DriverRequestActivityViewModel(): ViewModel() {

    fun llamarAtopico(key: String?, start_latitude: Double, start_longitude: Double, end_latitude: Double, end_longitude: Double, start: String?, end: String?, distance: String, time: String, mUser:FirebaseUser) {
        val requestQueue = Volley.newRequestQueue(MyApp.context)
        val json = JSONObject()
        try {
            json.put("to", "/topics/" + "trip_request")
            val notificacion = JSONObject()
            notificacion.put("id", mUser.uid)
            notificacion.put("key", key)
            notificacion.put("user_pos", "$start_latitude#$start_longitude")
            notificacion.put("end_pos", "$end_latitude#$end_longitude")
            notificacion.put("start", start)
            notificacion.put("end", end)
            notificacion.put("distance", distance)
            notificacion.put("time", time)
            json.put("data", notificacion)
            val url = "https://fcm.googleapis.com/fcm/send"
            val request: JsonObjectRequest = object : JsonObjectRequest(Method.POST, url, json, null, null) {
                override fun getHeaders(): Map<String, String> {
                    val header: MutableMap<String, String> = HashMap()
                    header["content-type"] = "application/json"
                    header["authorization"] = "key=AAAAF1VGxIE:APA91bG_6Ifrnq5Bh_Ts1b9JidvQiEb3KXotp9dxIipmdgYqzGSSUzKH0XOXdY3GSnMvAdIgHLJtP2FRDNQ1AHDf7W6lGSUEneq20Fl6xcvjXsCuup0CnaFbgQCRokYy3N0RhZdQKUrB"
                    return header
                }
            }
            requestQueue.add(request)
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }
    }

}