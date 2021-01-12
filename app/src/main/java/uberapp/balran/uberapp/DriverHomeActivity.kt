package uberapp.balran.uberapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.uberapp.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import uberapp.balran.uberapp.login.LoginActivity
import uberapp.balran.uberapp.pojos.Trip
import uberapp.balran.uberapp.utilities.Constantes
import uberapp.balran.uberapp.utilities.FindRoutes

class DriverHomeActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var viewmodel:DriverHomeActivityViewModel
    private var btnConnect: Button? = null
    private var btnCancelTrip: Button? = null
    private var ivDirections: ImageView? = null

    //Firebase variables
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var database: FirebaseDatabase? = null
    private var refClient: DatabaseReference? = null
    private var refUser: DatabaseReference? = null
    private var eventListener: ValueEventListener? = null
    private var refTrip: DatabaseReference? = null

    //Location Variables
    private var locationService: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var location: Location? = null
    private var userLocation: LatLng? = null
    private var destLocation: LatLng? = null
    private var auxLocation: LatLng? = null
    private var m: Marker? = null

    //Another variables
    private var saveLocation = false
    private var state = "disconnected"
    private var idClient: String? = null
    private var keyClient: String? = null
    private var ctx: Context? = null
    private var map: GoogleMap? = null
    private var findRoutes: FindRoutes? = null
    private var arrived = false
    private var tripCompleted = false
    var idClientCk: String? = null

    //polyline object
    private var polylines: MutableList<Polyline>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_driver_home)

        //Iniciando viewmodel
        viewmodel=ViewModelProvider(this).get(DriverHomeActivityViewModel::class.java)

        //Inicializando firebase
        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth!!.currentUser
        database = FirebaseDatabase.getInstance()
        refUser = database!!.getReference("Users").child("drivers").child(mUser!!.uid)
        FirebaseMessaging.getInstance().unsubscribeFromTopic("trip_request")

        //Inicializando la ubicacion
        locationService = LocationServices.getFusedLocationProviderClient(this)
        initUbication()
        getLastLocation()

        //Declaracion de variables
        A = this
        B = this
        ctx = this
        btnConnect = findViewById(R.id.btn_connect)
        btnCancelTrip = findViewById(R.id.btn_cancel_trip_driver)
        ivDirections = findViewById(R.id.iv_directions)
        ivDirections!!.visibility = View.GONE
        btnCancelTrip!!.visibility = View.GONE
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Iniciando mapas
        val mapFragment = fragmentManager.findFragmentById(R.id.map_driver) as MapFragment
        mapFragment.getMapAsync(this)

        //Metodos OnClick
        onClickMethods()
    } //Fin del oncreate

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.setMinZoomPreference(16f)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.exit) {
            Toast.makeText(this, "Cerrando...", Toast.LENGTH_SHORT).show()
            FirebaseAuth.getInstance().signOut()
            val i = Intent(this@DriverHomeActivity, LoginActivity::class.java)
            startActivity(i)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permiso aceptado, llamando a sendUbication
                    getLastLocation()
                } else {
                    //Permiso denegado
                    Toast.makeText(this, "Permiso denegado.", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    /*-------------------- CUSTOM METHODS ----------------------*/

    private fun onClickMethods() {
        btnConnect!!.setOnClickListener {
            if (state == "disconnected") {
                state = "connected"
                btnConnect!!.text = "Desconectarse"
                viewmodel.suscribeTopic()
            } else {
                state = "disconnected"
                btnConnect!!.text = "Conectarse"
                viewmodel.unSuscribeTopic()
            }
        }
        ivDirections!!.setOnClickListener { // Directions
            val from = location!!.latitude.toString() + ", " + location!!.longitude
            val dir = auxLocation!!.latitude.toString() + ", " + auxLocation!!.longitude
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                    "http://maps.google.com/maps?saddr=$from&daddr=$dir"))
            startActivity(intent)
        }
        btnCancelTrip!!.setOnClickListener { refTrip!!.child("state").setValue("canceled") }
    }

    private fun initUbication() {
        //Inicializando la ubicacion
        locationService = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest!!.interval = 10000
        locationRequest!!.fastestInterval = 5000
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    return
                }
                for (_location in locationResult.locations) {
                    location = _location

                    //Setting user position with marker
                    val ubi = LatLng(_location.latitude, _location.longitude)
                    if (m == null) {
                        val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_car2)
                        val marker = MarkerOptions().position(ubi)
                        marker.icon(icon)
                        m = map!!.addMarker(marker)
                        map!!.animateCamera(CameraUpdateFactory.newLatLng(ubi))
                    } else {
                        m!!.position = ubi
                        map!!.animateCamera(CameraUpdateFactory.newLatLng(ubi))
                    }
                    if (saveLocation) {
                        refUser!!.child("location").setValue(location!!.latitude.toString() + "#" + location!!.longitude)

                        //Checking if trip has been arrived
                        if (!arrived) {
                            val dist = viewmodel.distance(userLocation!!.latitude, userLocation!!.longitude, location!!.latitude, location!!.longitude)
                            if (dist < 50f) {
                                arrived = true
                                driverArrived(true)
                            }
                        }
                        if (!tripCompleted) {
                            val dist = viewmodel.distance(location!!.latitude, location!!.longitude, destLocation!!.latitude, destLocation!!.longitude)
                            if (dist < 50) {
                                refTrip = database!!.getReference("Trips").child(idClient!!).child(keyClient!!)
                                refTrip!!.child("state").setValue("completed")
                                tripCompleted = false
                            }
                        }
                    }
                }
            }
        }
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest!!)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(this) { // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            getLastLocation()
        }
        task.addOnFailureListener(this) { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@DriverHomeActivity, 1)
                } catch (sendEx: SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun driverArrived(firstArrived: Boolean) {
        map!!.clear()
        if (firstArrived) {
            refTrip!!.child("state").setValue("driving")
        }
        findRoutes = null
        polylines = null
        findRoutes = FindRoutes(ctx!!, B!!, map!!, polylines, userLocation!!, destLocation!!)
        findRoutes!!.findroutes(userLocation, destLocation)
        MyAsyncTask().execute()
        auxLocation = destLocation
        val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_car2)
        val marker = MarkerOptions().position(LatLng(location!!.latitude, location!!.longitude))
        marker.icon(icon)
        m = map!!.addMarker(marker)
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Si el permiso es denegado, se solicita el permiso.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION)
        } else {
            locationService!!.requestLocationUpdates(locationRequest,
                    locationCallback, Looper.getMainLooper())
        }
    }//El permiso esta aceptado, se procede a enviar la ubicacion

    //Si el permiso es denegado, se solicita el permiso.
    private fun getLastLocation(){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //Si el permiso es denegado, se solicita el permiso.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION)
            } else {
                //El permiso esta aceptado, se procede a enviar la ubicacion
                locationService!!.lastLocation.addOnSuccessListener { _location ->
                    if (_location != null) {
                        location = _location
                        checkIfHasTrip()
                        startLocationUpdates()
                        map!!.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location!!.latitude, location!!.longitude)))
                    } else {
                        Toast.makeText(this@DriverHomeActivity, "Error al obtener su ubicacion.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    fun receivedMessage(id: String?, key: String?, user_pos: String, end_pos: String, start: String, end: String, distance: String, time: String) {
        idClient = id
        keyClient = key
        //Iniciando Latlng del pasajero
        val dest = user_pos.split("#".toRegex()).toTypedArray()
        val latitude = dest[0]
        val longitude = dest[1]
        userLocation = LatLng(latitude.toDouble(), longitude.toDouble())
        //Iniciando Latlng del destino
        val endPosition = end_pos.split("#".toRegex()).toTypedArray()
        val endLatitude = endPosition[0]
        val endLongitude = endPosition[1]
        destLocation = LatLng(endLatitude.toDouble(), endLongitude.toDouble())
        //Mostrando notificacion
        showDialogTrip(start, end, distance, time)
    }

    private fun showDialogTrip(start: String, end: String, distance: String, time: String) {
        val mBuilder = AlertDialog.Builder(this@DriverHomeActivity)
        val v = layoutInflater.inflate(R.layout.dialog, null)


        val tvStart = v.findViewById<TextView>(R.id.tv_from)
        val tvEnd = v.findViewById<TextView>(R.id.tv_to)
        val tv_distance = v.findViewById<TextView>(R.id.tv_distance_dialog)
        val tv_time = v.findViewById<TextView>(R.id.tv_time_dialog)
        val btn_decline = v.findViewById<Button>(R.id.btn_decline)
        val btn_accept = v.findViewById<Button>(R.id.btn_accept)


        mBuilder.setView(v)
        val dialog = mBuilder.create()
        dialog.show()

        //Colocando valores de la notificacion
        tvStart.text = start
        tvEnd.text = end
        tv_distance.text = distance
        tv_time.text = time
        btn_decline.setOnClickListener {
            dialog.dismiss()
            idClient = null
            keyClient = null
            userLocation = null
        }
        btn_accept.setOnClickListener { tripAccepted(dialog) }
    }

    private fun showDialogFinished() {
        val mBuilder = AlertDialog.Builder(this@DriverHomeActivity)
        val v = layoutInflater.inflate(R.layout.dialog_finished, null)
        val btn_done = v.findViewById<Button>(R.id.btn_done)
        mBuilder.setCancelable(false)
        mBuilder.setView(v)
        val dialog = mBuilder.create()
        dialog.show()
        btn_done.setOnClickListener {
            dialog.dismiss()
            idClient = null
            keyClient = null
            userLocation = null
            destLocation = null
            polylines = null
            saveLocation = false
            arrived = false
            tripCompleted = false
            map!!.clear()
            m = null
            refUser!!.child("trip").setValue("null")
            viewmodel.suscribeTopic()
        }
    }

    private fun tripAccepted(dialog: AlertDialog?) {
        viewmodel.unSuscribeTopic()
        btnConnect!!.visibility = View.GONE
        btnConnect!!.text = "Desconectarse"
        state = "connected"
        refUser!!.child("trip").setValue(keyClient)
        refUser!!.child("trip_user").setValue(idClient)
        refClient = database!!.getReference("Users").child("clients").child(idClient!!)
        refTrip = database!!.getReference("Trips").child(idClient!!).child(keyClient!!)
        refTrip!!.child("id_driver").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val id = dataSnapshot.getValue(String::class.java)
                    if (id == "null" || id == mUser!!.uid) {
                        refTrip!!.child("id_driver").setValue(mUser!!.uid)
                        val start = LatLng(location!!.latitude, location!!.longitude)
                        if (dialog != null) {
                            findRoutes = FindRoutes(ctx!!, B!!, map!!, polylines, start, userLocation!!)
                            findRoutes!!.findroutes(start, userLocation)
                            MyAsyncTask().execute()
                        }
                        auxLocation = userLocation
                        dialog?.dismiss()
                        saveLocation = true
                        ivDirections!!.visibility = View.VISIBLE
                        btnCancelTrip!!.visibility = View.VISIBLE
                        Toast.makeText(this@DriverHomeActivity, "El viaje ha sido aceptado!", Toast.LENGTH_SHORT).show()
                    } else {
                        dialog?.dismiss()
                        idClient = null
                        keyClient = null
                        userLocation = null
                        Toast.makeText(this@DriverHomeActivity, "El viaje fue aceptado por otro chofer.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        if (dialog != null) {
            refTrip!!.child("state").setValue("comming")
        }
        eventListener = refTrip!!.child("state").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val state = snapshot.getValue(String::class.java)
                    if (state == "canceled") {
                        btnConnect!!.visibility = View.VISIBLE
                        map!!.clear()
                        idClient = null
                        keyClient = null
                        userLocation = null
                        findRoutes = null
                        arrived = false
                        tripCompleted = false
                        polylines = null
                        Toast.makeText(this@DriverHomeActivity, "El viaje fue cancelado.", Toast.LENGTH_SHORT).show()
                        refTrip!!.child("state").removeEventListener(eventListener!!)
                        refUser!!.child("trip").setValue("null")
                        refUser!!.child("trip_user").setValue("null")
                        viewmodel.suscribeTopic()
                        saveLocation = false
                        m = null
                        ivDirections!!.visibility = View.GONE
                        btnCancelTrip!!.visibility = View.GONE
                    } else if (state == "completed") {
                        btnConnect!!.visibility = View.VISIBLE
                        map!!.clear()
                        showDialogFinished()
                        refTrip!!.child("state").removeEventListener(eventListener!!)
                        refUser!!.child("trip").setValue("null")
                        refUser!!.child("trip_user").setValue("null")
                        m = null
                        arrived = false
                        tripCompleted = false
                        ivDirections!!.visibility = View.GONE
                        btnCancelTrip!!.visibility = View.GONE
                    } else if (state == "driving") {
                        driverArrived(false)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private inner class MyAsyncTask : AsyncTask<Void?, Void?, Void?>() {
        override fun onPostExecute(result: Void?) {}
        override fun doInBackground(vararg params: Void?): Void? {
            findRoutes!!.isFind
            return null
        }
    }

    private fun checkIfHasTrip() {
        refUser!!.child("trip_user").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                idClientCk = if (snapshot.exists()) {
                    val id = snapshot.getValue(String::class.java)
                    if (id != "null") {
                        id
                    } else {
                        return
                    }
                } else {
                    return
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        refUser!!.child("trip").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val id_trip = snapshot.getValue(String::class.java)
                    if (id_trip != "null" && idClientCk != null) {
                        val ref_trip = database!!.getReference("Trips").child(idClientCk!!).child(id_trip!!)
                        ref_trip.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val trip = snapshot.getValue(Trip::class.java)
                                    val startT = trip!!.start!!.split("#".toRegex()).toTypedArray()
                                    val start_lat = startT[0].toDouble()
                                    val start_long = startT[1].toDouble()
                                    val endT = trip.destination!!.split("#".toRegex()).toTypedArray()
                                    val end_lat = endT[0].toDouble()
                                    val end_long = endT[1].toDouble()
                                    idClient = trip.id_client
                                    keyClient = id_trip
                                    userLocation = LatLng(start_lat, start_long)
                                    destLocation = LatLng(end_lat, end_long)
                                    if (trip.state != "canceled") {
                                        tripAccepted(null)
                                    } else {
                                        keyClient = null
                                        idClient = null
                                        userLocation = null
                                        destLocation = null
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    companion object {
        var A: DriverHomeActivity? = null
        var B: Activity? = null
        fun comm(id: String?, key: String?, user_pos: String, end_pos: String, start: String, end: String, distance: String, time: String) {
            B!!.runOnUiThread { //Cambiar controles
                A!!.receivedMessage(id, key, user_pos, end_pos, start, end, distance, time)
            }
        }
    }
}