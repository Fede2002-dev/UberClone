package uberapp.balran.uberapp.clientHome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.uberapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import uberapp.balran.uberapp.utilities.FindRoutes

class TripActivity : AppCompatActivity(), OnMapReadyCallback {
    private var tv_driver_name: TextView? = null
    private var tv_matricula: TextView? = null
    private var tv_time: TextView? = null
    private var tv_distance: TextView? = null
    private var btn_cancel: Button? = null
    private var extra: Bundle? = null
    private var findRoutes: FindRoutes? = null
    private var ctx: Context? = null
    private var a: Activity? = null
    private val findroute = true
    var m: Marker? = null
    var state: String? = null

    //Firebase variables
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var database: FirebaseDatabase? = null
    var ref_trip: DatabaseReference? = null
    private var ref_user: DatabaseReference? = null
    var driverLoca: LatLng? = null

    //polyline object
    private val polylines: List<Polyline>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip)

        //Iniciar variables
        extra = intent.extras
        tv_driver_name = findViewById(R.id.tv_driver)
        tv_matricula = findViewById(R.id.tv_matricula)
        tv_time = findViewById(R.id.tv_time2)
        tv_distance = findViewById(R.id.tv_distance2)
        btn_cancel = findViewById(R.id.btn_cancel_trip)
        ctx = this
        a = this
        state = null


        //Iniciando firebase
        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth!!.currentUser
        database = FirebaseDatabase.getInstance()
        ref_trip = database!!.getReference("Trips").child(mUser!!.uid).child(extra!!.getString("trip_key")!!)
        ref_user = database!!.getReference("Users").child("clients").child(mUser!!.uid)

        //Iniciando mapa
        val mapFragment = fragmentManager.findFragmentById(R.id.map_trip) as MapFragment
        mapFragment.getMapAsync(this)

        //Colocando listener para estado del viaje
        ref_trip!!.child("state").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val trip_state = snapshot.getValue(String::class.java)
                    if (trip_state == "canceled") {
                        ref_user!!.child("trip").setValue("null")
                        val i = Intent(this@TripActivity, HomeActivity::class.java)
                        startActivity(i)
                        finish()
                    }
                    if (trip_state == "completed") {
                        ref_user!!.child("trip").setValue("null")
                        showDialogFinished()
                    }
                    if (trip_state == "driving") {
                        state = "driving"
                    }
                    if (trip_state == "comming") {
                        driverLoca = null
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        //Colocando datos del conductor
        setDriverData(extra!!.getString("driver"))

        //Metodos Onclick
        setOnClickMethods()
    }

    private fun setOnClickMethods() {
        btn_cancel!!.setOnClickListener { ref_trip!!.child("state").setValue("canceled") }
    }

    private fun setDriverData(driver: String?) {
        val ref_driver = database!!.getReference("Users").child("drivers").child(driver!!)
        ref_driver.child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.getValue(String::class.java)
                    Log.e("msg", name)
                    tv_driver_name!!.text = name
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        ref_driver.child("matricula").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val matricula = snapshot.getValue(String::class.java)
                    tv_matricula!!.text = matricula
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        getDriverLocation(driver)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        beginDriverLocation(extra!!.getString("driver"))
        map!!.setMinZoomPreference(15f)
    }

    private fun beginDriverLocation(id_driver: String?) {
        val ref_driver = database!!.getReference("Users").child("drivers").child(id_driver!!).child("location")
        ref_driver.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val driverlocation = dataSnapshot.getValue(String::class.java)
                    if (!driverlocation!!.isEmpty()) {
                        val loca = driverlocation.split("#".toRegex()).toTypedArray()
                        val latitude = loca[0]
                        val longitude = loca[1]
                        val driverLocation = arrayOf(latitude.toDouble(), longitude.toDouble())
                        if (!latitude.isEmpty() && !longitude.isEmpty()) {
                            if (driverLoca == null) {
                                driverLoca = LatLng(driverLocation[0], driverLocation[1])
                                //marcador conductor
                                val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_car2)
                                val marker = MarkerOptions().position(driverLoca!!)
                                marker.icon(icon)
                                m = map!!.addMarker(marker)
                                map!!.moveCamera(CameraUpdateFactory.newLatLng(driverLoca))
                            }
                            driverLoca = LatLng(driverLocation[0], driverLocation[1])
                            m!!.position = driverLoca!!
                            map!!.moveCamera(CameraUpdateFactory.newLatLng(driverLoca))
                            if (state != null) {
                                if (state == "driving") {
                                    map!!.clear()
                                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_car2)
                                    val marker = MarkerOptions().position(driverLoca!!)
                                    marker.icon(icon)
                                    m = map!!.addMarker(marker)
                                    val start = LatLng(extra!!.getDouble("start_lat"), extra!!.getDouble("start_long"))
                                    val end = LatLng(extra!!.getDouble("end_lat"), extra!!.getDouble("end_long"))
                                    findRoutes = FindRoutes(ctx, a, map, polylines, start, end)
                                    findRoutes!!.findroutes(start, end)
                                    state = null
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getDriverLocation(idDriver: String?) {
        val ref_driver = database!!.getReference("Users").child("drivers").child(idDriver!!).child("location")
        ref_driver.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val driverLocation = snapshot.getValue(String::class.java)
                    val loca = driverLocation!!.split("#".toRegex()).toTypedArray()
                    val latitude = loca[0]
                    val longitude = loca[1]
                    val start = LatLng(latitude.toDouble(), longitude.toDouble())
                    val end = LatLng(extra!!.getDouble("start_lat"), extra!!.getDouble("start_long"))
                    if (state != null) {
                        if (state != "driving") {
                            findRoutes = FindRoutes(ctx, a, map, polylines, start, end)
                            findRoutes!!.findroutes(start, end)
                            MyAsyncTask().execute()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showDialogFinished() {
        val mBuilder = AlertDialog.Builder(this@TripActivity)
        val v = layoutInflater.inflate(R.layout.dialog_finished, null)
        val btn_done = v.findViewById<Button>(R.id.btn_done)
        val tv_finished = v.findViewById<TextView>(R.id.tv_trip_finished)
        mBuilder.setCancelable(false)
        mBuilder.setView(v)
        val dialog = mBuilder.create()
        dialog.show()
        tv_finished.text = "Pague al conductor."
        btn_done.setOnClickListener {
            dialog.dismiss()
            val i = Intent(this@TripActivity, HomeActivity::class.java)
            startActivity(i)
            finish()
        }
    }

    private inner class MyAsyncTask : AsyncTask<Void?, Void?, Void?>() {

        override fun onPostExecute(result: Void?) {
            tv_distance!!.text = findRoutes!!.distance
            tv_time!!.text = findRoutes!!.time
        }

        override fun doInBackground(vararg params: Void?): Void? {
            findRoutes!!.isFind
            return null
        }
    }

    companion object {
        var map: GoogleMap? = null
    }
}