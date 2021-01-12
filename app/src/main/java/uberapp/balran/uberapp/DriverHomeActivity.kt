package uberapp.balran.uberapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import uberapp.balran.uberapp.login.LoginActivity;

import com.example.uberapp.R;

import uberapp.balran.uberapp.utilities.Constantes;
import uberapp.balran.uberapp.utilities.FindRoutes;
import uberapp.balran.uberapp.pojos.Trip;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;


public class DriverHomeActivity extends AppCompatActivity implements OnMapReadyCallback {
    private Button btn_connect, btn_cancel_trip;
    private ImageView iv_directions;

    //Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database;
    private DatabaseReference ref_client;
    private DatabaseReference ref_user;
    private ValueEventListener eventListener = null;
    private DatabaseReference ref_trip;

    //Location Variables
    private FusedLocationProviderClient locationService;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location location;
    private LatLng user_location;
    private LatLng dest_location;
    private LatLng aux_location;
    private Marker m;

    //Another variables
    private Boolean saveLocation = false;
    private String state = "disconnected";
    private String id_client;
    private String key_client;
    public static DriverHomeActivity A;
    public static Activity B;
    private Context ctx;
    private GoogleMap map;
    private FindRoutes findRoutes;
    private boolean arrived=false;
    private boolean tripCompleted=false;
    String id_client_ck;

    //polyline object
    private List<Polyline> polylines = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_driver_home);

        //Inicializando firebase

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        ref_user = database.getReference("Users").child("drivers").child(mUser.getUid());
        FirebaseMessaging.getInstance().unsubscribeFromTopic("trip_request");

        //Inicializando la ubicacion

        locationService = LocationServices.getFusedLocationProviderClient(this);
        initUbication();
        getLastLocation();

        //Declaracion de variables

        A = this;
        B = this;
        ctx=this;
        btn_connect = findViewById(R.id.btn_connect);
        btn_cancel_trip = findViewById(R.id.btn_cancel_trip_driver);
        iv_directions = findViewById(R.id.iv_directions);
        iv_directions.setVisibility(View.GONE);
        btn_cancel_trip.setVisibility(View.GONE);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Iniciando mapas

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_driver);
        mapFragment.getMapAsync(this);

        //Metodos OnClick

        OnClickMethods();


    }//Fin del oncreate

    private void suscribeTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("trip_request").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(DriverHomeActivity.this, "Conectado.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void unSuscribeTopic() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("trip_request").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(DriverHomeActivity.this, "Desconectado.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void OnClickMethods() {
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state.equals("disconnected")) {
                    state = "connected";
                    btn_connect.setText("Desconectarse");
                    suscribeTopic();
                } else {
                    state = "disconnected";
                    btn_connect.setText("Conectarse");
                    unSuscribeTopic();
                }
            }
        });

        iv_directions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Directions
                String from = location.getLatitude() + ", " + location.getLongitude();
                String dir = aux_location.latitude + ", " + aux_location.longitude;
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(
                        "http://maps.google.com/maps?saddr="+ from + "&daddr=" + dir));
                startActivity(intent);
            }
        });

        btn_cancel_trip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ref_trip.child("state").setValue("canceled");
            }
        });
    }

    private void initUbication() {
        //Inicializando la ubicacion

        locationService = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location _location : locationResult.getLocations()) {
                    location = _location;

                    //Setting user position with marker
                    LatLng ubi = new LatLng(_location.getLatitude(), _location.getLongitude());
                    if(m==null) {
                        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_car2);
                        MarkerOptions marker = new MarkerOptions().position(ubi);
                        marker.icon(icon);
                        m = map.addMarker(marker);
                        map.animateCamera(CameraUpdateFactory.newLatLng(ubi));
                    }else{
                        m.setPosition(ubi);
                        map.animateCamera(CameraUpdateFactory.newLatLng(ubi));
                    }
                    if (saveLocation) {
                        ref_user.child("location").setValue(location.getLatitude() + "#" + location.getLongitude());

                        //Checking if trip has been arrived
                        if(arrived==false){
                            Float dist = distance(user_location.latitude,user_location.longitude,location.getLatitude(),location.getLongitude());
                            if(dist<50f){
                                arrived=true;
                                driverArrived(true);
                            }
                        }
                        if(tripCompleted==false){
                            Float dist = distance(location.getLatitude(), location.getLongitude(), dest_location.latitude,dest_location.longitude);
                            if(dist<50){
                                ref_trip = database.getReference("Trips").child(id_client).child(key_client);
                                ref_trip.child("state").setValue("completed");
                                tripCompleted=false;
                            }
                        }

                    }

                }
            }
        };

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                getLastLocation();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(DriverHomeActivity.this, 1);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    private void driverArrived(Boolean firstArrived) {
        map.clear();
        if(firstArrived==true) {
            ref_trip.child("state").setValue("driving");
        }
        findRoutes=null;
        polylines=null;
        findRoutes = new FindRoutes(ctx,B,map,polylines, user_location, dest_location);
        findRoutes.findroutes(user_location, dest_location);
        new MyAsyncTask().execute();
        aux_location = dest_location;

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_car2);
        MarkerOptions marker = new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude()));
        marker.icon(icon);
        m = map.addMarker(marker);
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Si el permiso es denegado, se solicita el permiso.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            locationService.requestLocationUpdates(locationRequest,
                    locationCallback, Looper.getMainLooper());
        }
    }


    private void getLastLocation(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Si el permiso es denegado, se solicita el permiso.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constantes.MY_PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            //El permiso esta aceptado, se procede a enviar la ubicacion
            locationService.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location _location) {
                    if (_location != null) {
                        location = _location;
                        checkIfHasTrip();
                        startLocationUpdates();
                        map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                    } else {
                        Toast.makeText(DriverHomeActivity.this, "Error al obtener su ubicacion.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public static void comm(final String id, final String key, final String user_pos,final String end_pos, final String start, final String end, final String distance, final String time) {
        B.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Cambiar controles
                A.receivedMessage(id, key, user_pos,end_pos, start, end, distance, time);
            }
        });
    }

    public void receivedMessage(String id, String key, String user_pos,String end_pos, String start, String end, String distance,String time) {

        id_client = id;
        key_client = key;
        //Iniciando Latlng del pasajero
        String[] dest = user_pos.split("#");
        String latitude = dest[0];
        String longitude = dest[1];
        user_location = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
        //Iniciando Latlng del destino
        String[] end_position = end_pos.split("#");
        String end_latitude = end_position[0];
        String end_longitude = end_position[1];
        dest_location = new LatLng(Double.parseDouble(end_latitude), Double.parseDouble(end_longitude));
        //Mostrando notificacion
        showDialogTrip(start,end,distance,time);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(16);
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
            Intent i = new Intent(DriverHomeActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialogTrip(String start, String end, String distance, String time) {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(DriverHomeActivity.this);
        View v = getLayoutInflater().inflate(R.layout.dialog, null);

        TextView tv_start = v.findViewById(R.id.tv_from);
        TextView tv_end = v.findViewById(R.id.tv_to);
        TextView tv_distance = v.findViewById(R.id.tv_distance_dialog);
        TextView tv_time = v.findViewById(R.id.tv_time_dialog);
        Button btn_decline = v.findViewById(R.id.btn_decline);
        Button btn_accept = v.findViewById(R.id.btn_accept);

        mBuilder.setView(v);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        //Colocando valores de la notificacion
        tv_start.setText(start);
        tv_end.setText(end);
        tv_distance.setText(distance);
        tv_time.setText(time);

        btn_decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                id_client = null;
                key_client = null;
                user_location = null;
            }
        });

        btn_accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tripAccepted(dialog);
            }
        });
    }

    private void showDialogFinished(){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(DriverHomeActivity.this);
        View v = getLayoutInflater().inflate(R.layout.dialog_finished,null);

        Button btn_done = v.findViewById(R.id.btn_done);

        mBuilder.setCancelable(false);
        mBuilder.setView(v);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                id_client = null;
                key_client = null;
                user_location = null;
                dest_location=null;
                polylines=null;
                saveLocation=false;
                arrived=false;
                tripCompleted=false;
                map.clear();
                m=null;
                ref_user.child("trip").setValue("null");

                suscribeTopic();
            }
        });
    }

    private void tripAccepted(final AlertDialog dialog) {
        unSuscribeTopic();

        btn_connect.setVisibility(View.GONE);
        btn_connect.setText("Desconectarse");
        state = "connected";
        ref_user.child("trip").setValue(key_client);
        ref_user.child("trip_user").setValue(id_client);

        ref_client = database.getReference("Users").child("clients").child(id_client);

        ref_trip = database.getReference("Trips").child(id_client).child(key_client);

        ref_trip.child("id_driver").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String id = dataSnapshot.getValue(String.class);
                    if (id.equals("null") || id.equals(mUser.getUid())) {
                        ref_trip.child("id_driver").setValue(mUser.getUid());
                        LatLng start = new LatLng(location.getLatitude(), location.getLongitude());
                        if(dialog!=null) {
                            findRoutes = new FindRoutes(ctx, B, map, polylines, start, user_location);
                            findRoutes.findroutes(start, user_location);
                            new MyAsyncTask().execute();
                        }
                        aux_location = user_location;
                        if(dialog!=null) {
                            dialog.dismiss();
                        }
                        saveLocation=true;
                        iv_directions.setVisibility(View.VISIBLE);
                        btn_cancel_trip.setVisibility(View.VISIBLE);
                        Toast.makeText(DriverHomeActivity.this, "El viaje ha sido aceptado!", Toast.LENGTH_SHORT).show();
                    }else{
                        if(dialog!=null) {
                            dialog.dismiss();
                        }
                        id_client = null;
                        key_client = null;
                        user_location = null;
                        Toast.makeText(DriverHomeActivity.this, "El viaje fue aceptado por otro chofer.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(dialog!=null) {
            ref_trip.child("state").setValue("comming");
        }

        eventListener=ref_trip.child("state").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String state = snapshot.getValue(String.class);
                    if(state.equals("canceled")){
                        btn_connect.setVisibility(View.VISIBLE);
                        map.clear();
                        id_client = null;
                        key_client = null;
                        user_location = null;
                        findRoutes = null;
                        arrived=false;
                        tripCompleted=false;
                        polylines=null;
                        Toast.makeText(DriverHomeActivity.this, "El viaje fue cancelado.", Toast.LENGTH_SHORT).show();
                        ref_trip.child("state").removeEventListener(eventListener);
                        ref_user.child("trip").setValue("null");
                        ref_user.child("trip_user").setValue("null");
                        suscribeTopic();
                        saveLocation=false;
                        m=null;
                        iv_directions.setVisibility(View.GONE);
                        btn_cancel_trip.setVisibility(View.GONE);
                    }
                    else if(state.equals("completed")){
                        btn_connect.setVisibility(View.VISIBLE);
                        map.clear();
                        showDialogFinished();
                        ref_trip.child("state").removeEventListener(eventListener);
                        ref_user.child("trip").setValue("null");
                        ref_user.child("trip_user").setValue("null");
                        m=null;
                        arrived=false;
                        tripCompleted=false;
                        iv_directions.setVisibility(View.GONE);
                        btn_cancel_trip.setVisibility(View.GONE);
                    }

                    else if(state.equals("driving")){
                        driverArrived(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private class MyAsyncTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params) {
            findRoutes.isFind();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {

        }
    }

    public float distance (Double lat_a, Double lng_a, Double lat_b, Double lng_b )
    {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue();
    }

    private void checkIfHasTrip() {
        ref_user.child("trip_user").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String id = snapshot.getValue(String.class);
                    if(!id.equals("null")){
                        id_client_ck = id;
                    }else{return;}
                }else{
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        ref_user.child("trip").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    final String id_trip = snapshot.getValue(String.class);
                    if(!id_trip.equals("null") && id_client_ck!=null){
                        final DatabaseReference ref_trip = database.getReference("Trips").child(id_client_ck).child(id_trip);
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

                                    id_client = trip.getId_client();
                                    key_client = id_trip;
                                    user_location = new LatLng(start_lat, start_long);
                                    dest_location = new LatLng(end_lat, end_long);

                                    if(!trip.getState().equals("canceled")) {
                                        tripAccepted(null);
                                    }else{
                                        key_client=null;
                                        id_client=null;
                                        user_location=null;
                                        dest_location=null;
                                    }
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
                    getLastLocation();
                    
                } else {
                    //Permiso denegado
                    Toast.makeText(this, "Permiso denegado.", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }
}
