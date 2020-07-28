package com.example.uberapp.ClientHome;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.uberapp.DriverHomeActivity;
import com.example.uberapp.Utilities.Constantes;
import com.example.uberapp.Login.LoginActivity;
import com.example.uberapp.R;
import com.example.uberapp.pojos.Trip;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {
    private Button btn_requestDriver;
    private SearchView sv_location;
    private ImageView iv_gps;
    Toolbar toolbar;

    //Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database;
    private DatabaseReference ref_user;

    //Ubication variables
    private FusedLocationProviderClient locationService;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Boolean saveLocation = false;
    private LatLng location;
    LatLng driverLoca;
    LatLng destination;
    View mapView;
    //google map object
    private GoogleMap map;

    //current and destination location objects
    protected LatLng start = null;
    protected LatLng end = null;

    //polyline object
    private List<Polyline> polylines = null;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //Inicializando la ubicacion
        locationService = LocationServices.getFusedLocationProviderClient(this);
        requestPermissions();

        //Iniciando mapa
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapView = mapFragment.getView();

        //Inicializando firebase

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        ref_user = database.getReference("Users").child("clients").child(mUser.getUid());
        checkIfHasTrip();

        //Declaracion de variables

        btn_requestDriver = findViewById(R.id.btn_requestDriver);
        sv_location = findViewById(R.id.sv_location);
        iv_gps = findViewById(R.id.iv_mylocation);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sv_location.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String s_location = sv_location.getQuery().toString();
                List<Address> addressList = null;

                if (s_location != null || !location.equals("")) {
                    Geocoder geocoder = new Geocoder(HomeActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(s_location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        map.clear();
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        destination = latLng;
                        map.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination)).draggable(true).flat(true));
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
                    } catch (Exception e) {
                        Toast.makeText(HomeActivity.this, "No se ha encontrado la ubicacion.", Toast.LENGTH_SHORT).show();
                    }

                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        //onClicks

        iv_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.animateCamera(CameraUpdateFactory.newLatLng(location));
            }
        });

        btn_requestDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (destination != null) {

                    start = location;
                    end = destination;
                    String origin = getAddress(start.latitude, start.longitude);
                    String adr_destination = getAddress(end.latitude, end.longitude);

                    Intent i = new Intent(HomeActivity.this, DriverRequestActivity.class);

                    i.putExtra("start_lat", start.latitude);
                    i.putExtra("start_long", start.longitude);
                    i.putExtra("end_lat", end.latitude);
                    i.putExtra("end_long", end.longitude);
                    i.putExtra("origin", origin);
                    i.putExtra("destination", adr_destination);
                    startActivity(i);
                } else {
                    Toast.makeText(HomeActivity.this, "Seleccione una ubicacion", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }//fin del oncreate

    private void checkIfHasTrip() {
        ref_user.child("trip").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    final String id_trip = snapshot.getValue(String.class);
                    if(!id_trip.equals("null")){
                        DatabaseReference ref_trip = database.getReference("Trips").child(mUser.getUid()).child(id_trip);
                        ref_trip.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists()){
                                    Trip trip = snapshot.getValue(Trip.class);

                                    String[] startT = trip.getStart().split("#");
                                    Double start_lat = Double.parseDouble(startT[0]);
                                    Double start_long = Double.parseDouble(startT[1]);

                                    String[]endT = trip.getDestination().split("#");
                                    Double end_lat = Double.parseDouble(endT[0]);
                                    Double end_long = Double.parseDouble(endT[1]);

                                    String ori = getAddress(start_lat, start_long);
                                    String desti = getAddress(end_lat, end_long);

                                    Intent i = new Intent(HomeActivity.this, TripActivity.class);

                                    i.putExtra("start_lat", start_lat);
                                    i.putExtra("start_long", start_long);
                                    i.putExtra("end_lat", end_lat);
                                    i.putExtra("end_long", end_long);
                                    i.putExtra("origin", ori);
                                    i.putExtra("destination", desti);
                                    i.putExtra("trip_key", id_trip);
                                    i.putExtra("driver", trip.getId_driver());
                                    startActivity(i);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permiso aceptado, llamando a sendUbication
                } else {
                    //Permiso denegado
                    Toast.makeText(this, "Permiso denegado.", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.exit) {
            Toast.makeText(this, "Cerrando...", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(17f);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);


        getMyLocation();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Si el permiso es denegado, se solicita el permiso.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            map.setMyLocationEnabled(true);
        }
    }


    private void getMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Si el permiso es denegado, se solicita el permiso.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            locationService.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location _location) {
                    if (_location != null) {
                        location = new LatLng(_location.getLatitude(), _location.getLongitude());
                        map.animateCamera(CameraUpdateFactory.newLatLng(location));
                    } else {
                        Toast.makeText(HomeActivity.this, "Error al obtener la ubicacion.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public String getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(HomeActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            String add = obj.getAddressLine(0);
            add = add + "\n" + obj.getCountryName();
            add = add + "\n" + obj.getCountryCode();
            add = add + "\n" + obj.getAdminArea();
            add = add + "\n" + obj.getPostalCode();
            add = add + "\n" + obj.getSubAdminArea();
            add = add + "\n" + obj.getLocality();
            add = add + "\n" + obj.getThoroughfare();
            add = add + "\n" + obj.getSubThoroughfare();

            Log.e("IGA", "Address" + add);

            String addr = obj.getThoroughfare() + " " + obj.getSubThoroughfare() + ", " + obj.getLocality();
            return addr;
            // Toast.makeText(this, "Address=>" + add,
            // Toast.LENGTH_SHORT).show();

            // TennisAppActivity.showDialog(add);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e("err", "err");
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Si el permiso es denegado, se solicita el permiso.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            getMyLocation();
        }
    }
}