package uberapp.balran.uberapp.utilities

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import uberapp.balran.uberapp.DriverHomeActivity

class FCM : FirebaseMessagingService() {
    var a: DriverHomeActivity? = null
    override fun onNewToken(s: String) {
        super.onNewToken(s)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (remoteMessage.data.isNotEmpty()) {
            if (DriverHomeActivity.A != null) {
                val destination = remoteMessage.data["user_pos"]
                val endPos = remoteMessage.data["end_pos"]
                val key = remoteMessage.data["key"]
                val id = remoteMessage.data["id"]
                val start = remoteMessage.data["start"]
                val end = remoteMessage.data["end"]
                val distance = remoteMessage.data["distance"]
                val time = remoteMessage.data["time"]
                a = DriverHomeActivity.A
                DriverHomeActivity.comm(id, key, destination!!, endPos!!, start!!, end!!, distance!!, time!!)
            }
        }
    }
}