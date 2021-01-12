package uberapp.balran.uberapp.clientHome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.uberapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
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
import uberapp.balran.uberapp.login.LoginActivity
import uberapp.balran.uberapp.utilities.Constantes
import uberapp.balran.uberapp.pojos.Trip
import java.io.IOException
import java.util.*

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var btnRequestdriver: Button
    private lateinit var svLocation: SearchView
    private lateinit var ivGps: ImageView
    lateinit var toolbar: Toolbar

    //Firebase variables
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var database: FirebaseDatabase? = null
    private var ref_user: DatabaseReference? = null

    //Ubication variables
    private var locationService: FusedLocationProviderClient? = null
    private var location: LatLng? = null
    var destination: LatLng? = null
    var mapView: View? = null

    //google map object
    private var map: GoogleMap? = null

    //current and destination location objects
    protected var start: LatLng? = null
    protected var end: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_home)

        //Inicializando la ubicacion
        locationService = LocationServices.getFusedLocationProviderClient(this)
        requestPermissions()

        //Iniciando mapa
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)
        mapView = mapFragment.view

        //Inicializando firebase
        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth!!.currentUser
        database = FirebaseDatabase.getInstance()
        ref_user = database!!.getReference("Users").child("clients").child(mUser!!.uid)
        checkIfHasTrip()

        //Declaracion de variables
        btnRequestdriver = findViewById(R.id.btn_requestDriver)
        svLocation = findViewById(R.id.sv_location)
        ivGps = findViewById(R.id.iv_mylocation)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        svLocation.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val sLocation = svLocation.query.toString()
                var addressList: List<Address>? = null
                if (sLocation != null || location != null) {
                    val geocoder = Geocoder(this@HomeActivity)
                    try {
                        addressList = geocoder.getFromLocationName(sLocation, 1)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    try {
                        map!!.clear()
                        val address = addressList!![0]
                        val latLng = LatLng(address.latitude, address.longitude)
                        destination = latLng
                        map!!.addMarker(MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination)).draggable(true).flat(true))
                        map!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                    } catch (e: Exception) {
                        Toast.makeText(this@HomeActivity, "No se ha encontrado la ubicacion.", Toast.LENGTH_SHORT).show()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })


        //onClicks
        ivGps.setOnClickListener{map!!.animateCamera(CameraUpdateFactory.newLatLng(location))}
        btnRequestdriver.setOnClickListener{
            if (destination != null) {
                start = location
                end = destination
                val origin = getAddress(start!!.latitude, start!!.longitude)
                val adrDestination = getAddress(end!!.latitude, end!!.longitude)
                val i = Intent(this@HomeActivity, DriverRequestActivity::class.java)
                i.putExtra("start_lat", start!!.latitude)
                i.putExtra("start_long", start!!.longitude)
                i.putExtra("end_lat", end!!.latitude)
                i.putExtra("end_long", end!!.longitude)
                i.putExtra("origin", origin)
                i.putExtra("destination", adrDestination)
                startActivity(i)
            } else {
                Toast.makeText(this@HomeActivity, "Seleccione una ubicacion", Toast.LENGTH_SHORT).show()
            }
        }
    } //fin del oncreate

    private fun checkIfHasTrip() {
        ref_user!!.child("trip").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val id_trip = snapshot.getValue(String::class.java)
                    if (id_trip != "null") {
                        val refTrip = database!!.getReference("Trips").child(mUser!!.uid).child(id_trip!!)
                        refTrip.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val trip = snapshot.getValue(Trip::class.java)
                                    val startT = trip!!.start.split("#".toRegex()).toTypedArray()
                                    val start_lat = startT[0].toDouble()
                                    val start_long = startT[1].toDouble()
                                    val endT = trip.destination.split("#".toRegex()).toTypedArray()
                                    val end_lat = endT[0].toDouble()
                                    val end_long = endT[1].toDouble()
                                    val ori = getAddress(start_lat, start_long)
                                    val desti = getAddress(end_lat, end_long)
                                    val i = Intent(this@HomeActivity, TripActivity::class.java)
                                    i.putExtra("start_lat", start_lat)
                                    i.putExtra("start_long", start_long)
                                    i.putExtra("end_lat", end_lat)
                                    i.putExtra("end_long", end_long)
                                    i.putExtra("origin", ori)
                                    i.putExtra("destination", desti)
                                    i.putExtra("trip_key", id_trip)
                                    i.putExtra("driver", trip.id_driver)
                                    startActivity(i)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permiso aceptado, llamando a sendUbication
                    getMyLocation(true)
                } else {
                    //Permiso denegado
                    Toast.makeText(this, "Permiso denegado.", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
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
            val i = Intent(this@HomeActivity, LoginActivity::class.java)
            startActivity(i)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map!!.setMinZoomPreference(17f)
        map!!.uiSettings.isRotateGesturesEnabled = false
        map!!.uiSettings.isMyLocationButtonEnabled = false
        map!!.setOnMyLocationChangeListener { getMyLocation(false) }
        getMyLocation(true)
    }

    private fun getMyLocation(camera: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Si el permiso es denegado, se solicita el permiso.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION)
        } else {
            if (map != null) {
                map!!.isMyLocationEnabled = true
            }
            locationService!!.lastLocation.addOnSuccessListener { _location ->
                if (_location != null) {
                    location = LatLng(_location.latitude, _location.longitude)
                    if (camera) {
                        map!!.animateCamera(CameraUpdateFactory.newLatLng(location))
                    }
                } else {
                    Toast.makeText(this@HomeActivity, "Error al obtener la ubicacion.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getAddress(lat: Double, lng: Double): String? {
        val geocoder = Geocoder(this@HomeActivity, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val obj = addresses[0]
            return obj.thoroughfare + " " + obj.subThoroughfare + ", " + obj.locality
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
        return null
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Si el permiso es denegado, se solicita el permiso.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION)
        } else {
            getMyLocation(true)
        }
    }
}