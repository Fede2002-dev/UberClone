package uberapp.balran.uberapp.clientHome

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.uberapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import org.json.JSONException
import org.json.JSONObject
import uberapp.balran.uberapp.utilities.FindRoutes
import uberapp.balran.uberapp.pojos.Trip
import java.util.*

class DriverRequestActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var tvOrigin: TextView
    private lateinit var tvDestination: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDistance: TextView
    private lateinit var extra: Bundle
    private lateinit var btnConfirmTrip: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var findRoutes: FindRoutes
    private var cancelTrip = false

    //Firebase variables
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mUser: FirebaseUser
    private lateinit var database: FirebaseDatabase
    private lateinit var refUser: DatabaseReference
    private lateinit var refTrip: DatabaseReference
    private lateinit var listener: ValueEventListener
    var keyPush: String? = ""
    lateinit var refIdDriver: DatabaseReference

    //current and destination location objects
    var start: LatLng?=null
    var end: LatLng?=null
    //polyline object
    private var polylines: List<Polyline>? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_request)

        //Iniciar variables
        extra = intent.extras!!
        tvOrigin = findViewById(R.id.tv_origen)
        tvDestination = findViewById(R.id.tv_destination)
        tvTime = findViewById(R.id.tv_time)
        tvDistance = findViewById(R.id.tv_distance)
        btnConfirmTrip = findViewById(R.id.btn_confirm_trip)
        progressBar = findViewById(R.id.progressBar)

        //Inicializando firebase
        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth.currentUser!!
        database = FirebaseDatabase.getInstance()
        refUser = database.getReference("Users").child("clients").child(mUser.uid)
        refTrip = database.getReference("Trips").child(mUser.uid)

        //Setear valores
        tvOrigin.text= extra.getString("origin")
        tvDestination.text= extra.getString("destination")
        progressBar.visibility = View.GONE

        //Iniciando mapa
        val mapFragment = this.fragmentManager.findFragmentById(R.id.map_driver_request) as MapFragment
        mapFragment.getMapAsync(this)

        //Metodos OnClick
        onClickMethods()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.setMinZoomPreference(15f)

        //Inicializar variables
        start = LatLng(extra.getDouble("start_lat"), extra.getDouble("start_long"))
        end = LatLng(extra.getDouble("end_lat"), extra.getDouble("end_long"))
        if (start != null && end != null) {
            map!!.animateCamera(CameraUpdateFactory.newLatLng(end))
            findRoutes = FindRoutes(this, this, map, polylines, start, end)
            findRoutes.findroutes(start, end)
            MyAsyncTask().execute()
        } else {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        }
    }

    //Custom methods
    private inner class MyAsyncTask : AsyncTask<Void?, Void?, Void?>() {

        override fun onPostExecute(result: Void?) {
            tvDistance.text = findRoutes.distance
            tvTime.text = findRoutes.time
            val startMarker = MarkerOptions()
            startMarker.position(start!!)
            startMarker.title("Inicio")
            startMarker.flat(true)
            startMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_client))
            map!!.addMarker(startMarker)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            findRoutes.isFind
            return null
        }
    }

    private fun onClickMethods() {
        btnConfirmTrip.setOnClickListener {
            if (cancelTrip == true) {
                Toast.makeText(this@DriverRequestActivity, "Cancelando", Toast.LENGTH_SHORT).show()
                val r = database.getReference("Trips").child(mUser.uid).child(keyPush!!)
                r.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            r.removeValue()
                            refIdDriver.removeEventListener(listener)
                            keyPush = ""
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
                val i = Intent(this@DriverRequestActivity, HomeActivity::class.java)
                startActivity(i)
                finish()
            } else {
                cancelTrip = true
                progressBar.visibility = View.VISIBLE
                btnConfirmTrip.text = "Cancelar viaje"
                val trip = Trip("null", mUser.uid, end!!.latitude.toString() + "#" + end!!.longitude, start!!.latitude.toString() + "#" + start!!.longitude, "waiting")
                keyPush = refTrip.push().key
                refTrip.child(keyPush!!).setValue(trip)
                refUser.child("trip").setValue(keyPush)
                llamarAtopico(keyPush, start!!.latitude, start!!.longitude, end!!.latitude, end!!.longitude, extra.getString("origin"), extra.getString("destination"), findRoutes.distance, findRoutes.time)
                refIdDriver = refTrip.child(keyPush!!).child("id_driver")
                listener = refIdDriver.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val idDriver = dataSnapshot.getValue(String::class.java)!!
                            if (idDriver != "null") {
                                refTrip.child(keyPush!!).child("state").setValue("confirmed")
                                refIdDriver.removeEventListener(listener)
                                val i = Intent(this@DriverRequestActivity, TripActivity::class.java)
                                i.putExtra("start_lat", start!!.latitude)
                                i.putExtra("start_long", start!!.longitude)
                                i.putExtra("end_lat", end!!.latitude)
                                i.putExtra("end_long", end!!.longitude)
                                i.putExtra("trip_key", keyPush)
                                i.putExtra("origin", extra.getString("origin"))
                                i.putExtra("destination", extra.getString("destination"))
                                i.putExtra("driver", idDriver)
                                startActivity(i)
                                finish()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun llamarAtopico(key: String?, start_latitude: Double, start_longitude: Double, end_latitude: Double, end_longitude: Double, start: String?, end: String?, distance: String, time: String) {
        val requestQueue = Volley.newRequestQueue(applicationContext)
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
            val URL = "https://fcm.googleapis.com/fcm/send"
            val request: JsonObjectRequest = object : JsonObjectRequest(Method.POST, URL, json, null, null) {
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

    override fun onDestroy() {
        /*if(!key_push.equals("")) {
            final DatabaseReference r = database.getReference("Trips").child(mUser.getUid()).child(key_push);
            r.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        r.removeValue();
                        ref_id_driver.removeEventListener(listener);
                        key_push =null;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }*/
        super.onDestroy()
    }

    companion object {
        var map: GoogleMap? = null
    }
}