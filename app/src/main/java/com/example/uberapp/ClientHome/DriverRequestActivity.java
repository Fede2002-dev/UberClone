package com.example.uberapp.ClientHome;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.example.uberapp.R;
import com.example.uberapp.Utilities.FindRoutes;
import com.example.uberapp.pojos.Trip;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverRequestActivity extends AppCompatActivity implements OnMapReadyCallback{
    public static GoogleMap map;
    private TextView tv_origin, tv_destination, tv_time, tv_distance;
    private Bundle extra;
    private Button btn_confirm_trip;
    private ProgressBar progressBar;
    private FindRoutes findRoutes;
    private Boolean cancelTrip=false;

    //Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database;
    private DatabaseReference ref_user;
    private DatabaseReference ref_trip;
    private ValueEventListener listener;
    String key_push = "";
    DatabaseReference ref_id_driver;

    //current and destination location objects
    protected LatLng start=null;
    protected LatLng end=null;

    //polyline object
    private List<Polyline> polylines=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_request);

        //Iniciar variables
        extra = getIntent().getExtras();
        tv_origin = findViewById(R.id.tv_origen);
        tv_destination = findViewById(R.id.tv_destination);
        tv_time = findViewById(R.id.tv_time);
        tv_distance = findViewById(R.id.tv_distance);
        btn_confirm_trip = findViewById(R.id.btn_confirm_trip);
        progressBar = findViewById(R.id.progressBar);

        //Inicializando firebase

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        ref_user = database.getReference("Users").child("clients").child(mUser.getUid());
        ref_trip = database.getReference("Trips").child(mUser.getUid());

        //Setear valores
        tv_origin.setText(extra.getString("origin"));
        tv_destination.setText(extra.getString("destination"));
        progressBar.setVisibility(View.GONE);

        //Iniciando mapa
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_driver_request);
        mapFragment.getMapAsync(this);

        //Metodos OnClick
        onClickMethods();
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(10);

        //Inicializar variables
        start= new LatLng(extra.getDouble("start_lat"), extra.getDouble("start_long"));
        end= new LatLng(extra.getDouble("end_lat"), extra.getDouble("end_long"));

        if(start!=null && end !=null) {
            map.animateCamera(CameraUpdateFactory.newLatLng(end));
            findRoutes = new FindRoutes(this,this,map,polylines, start, end);
            findRoutes.findroutes(start, end);
            new MyAsyncTask().execute();
        }
        else{
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    //Custom methods
    private class MyAsyncTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params) {
            findRoutes.isFind();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            tv_distance.setText(findRoutes.getDistance());
            tv_time.setText(findRoutes.getTime());

            MarkerOptions startMarker = new MarkerOptions();
            startMarker.position(start);
            startMarker.title("Inicio");
            startMarker.flat(true);
            startMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_client));
            map.addMarker(startMarker);
        }
    }

    private void onClickMethods() {
        btn_confirm_trip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cancelTrip==true){
                    Toast.makeText(DriverRequestActivity.this, "Cancelando", Toast.LENGTH_SHORT).show();
                    final DatabaseReference r = database.getReference("Trips").child(mUser.getUid()).child(key_push);
                    r.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                r.removeValue();
                                ref_id_driver.removeEventListener(listener);
                                key_push ="";
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    Intent i = new Intent(DriverRequestActivity.this, HomeActivity.class);
                    startActivity(i);
                    finish();
                }else {
                    cancelTrip = true;
                    progressBar.setVisibility(View.VISIBLE);
                    btn_confirm_trip.setText("Cancelar viaje");

                    Trip trip = new Trip("null",mUser.getUid(),  end.latitude + "#" + end.longitude, start.latitude+"#"+start.longitude, "waiting");

                    key_push = ref_trip.push().getKey();
                    ref_trip.child(key_push).setValue(trip);

                    ref_user.child("trip").setValue(key_push);

                    Log.e("LOgn", String.valueOf(start.longitude));
                    Log.e("LOgn", String.valueOf(start.latitude));
                    llamarAtopico(key_push,start.latitude, start.longitude, end.latitude, end.longitude, extra.getString("origin"), extra.getString("destination"), findRoutes.getDistance(), findRoutes.getTime());

                    ref_id_driver = ref_trip.child(key_push).child("id_driver");
                    listener = ref_id_driver.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                String id_driver= dataSnapshot.getValue(String.class);
                                assert id_driver != null;
                                if(!id_driver.equals("null")){
                                    ref_trip.child(key_push).child("state").setValue("confirmed");
                                    Toast.makeText(DriverRequestActivity.this, id_driver, Toast.LENGTH_SHORT).show();
                                    ref_id_driver.removeEventListener(listener);
                                    Intent i = new Intent(DriverRequestActivity.this, TripActivity.class);
                                    i.putExtra("start_lat", start.latitude);
                                    i.putExtra("start_long", start.longitude);
                                    i.putExtra("end_lat", end.latitude);
                                    i.putExtra("end_long", end.longitude);
                                    i.putExtra("trip_key", key_push);
                                    i.putExtra("origin", extra.getString("origin"));
                                    i.putExtra("destination", extra.getString("destination"));
                                    i.putExtra("driver", id_driver);
                                    startActivity(i);
                                    finish();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {

                        }
                    });
                }
            }
        });
    }


    private void llamarAtopico(String key, Double start_latitude, Double start_longitude, Double end_latitude, Double end_longitude,String start, String end, String distance, String time) {

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        JSONObject json = new JSONObject();

        try {

            json.put("to", "/topics/" + "trip_request");
            JSONObject notificacion = new JSONObject();
            notificacion.put("id", mUser.getUid());
            notificacion.put("key", key);
            notificacion.put("user_pos", start_latitude + "#" + start_longitude);
            notificacion.put("end_pos", end_latitude + "#" + end_longitude);
            notificacion.put("start", start);
            notificacion.put("end",end);
            notificacion.put("distance", distance);
            notificacion.put("time",time);

            json.put("data", notificacion);

            String URL = "https://fcm.googleapis.com/fcm/send";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,  URL, json,null, null){
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> header = new HashMap<>();

                    header.put("content-type", "application/json");
                    header.put("authorization","key=AAAAF1VGxIE:APA91bG_6Ifrnq5Bh_Ts1b9JidvQiEb3KXotp9dxIipmdgYqzGSSUzKH0XOXdY3GSnMvAdIgHLJtP2FRDNQ1AHDf7W6lGSUEneq20Fl6xcvjXsCuup0CnaFbgQCRokYy3N0RhZdQKUrB");
                    return header;
                }
            };

            requestQueue.add(request);


        }catch (JSONException ex){
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
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
        super.onDestroy();
    }
}