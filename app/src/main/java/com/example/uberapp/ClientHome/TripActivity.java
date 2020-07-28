package com.example.uberapp.ClientHome;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberapp.DriverHomeActivity;
import com.example.uberapp.R;
import com.example.uberapp.Utilities.Constantes;
import com.example.uberapp.Utilities.FindRoutes;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class TripActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static GoogleMap map;
    private TextView tv_driver_name, tv_matricula, tv_time, tv_distance;
    private Button btn_cancel;
    private Bundle extra;
    private FindRoutes findRoutes;
    private Context ctx;
    private Activity a;
    private Boolean findroute = true;
    Marker m;

    //Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database;
    DatabaseReference ref_trip;
    private DatabaseReference ref_user;

    LatLng driverLoca;

    //polyline object
    private List<Polyline> polylines=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        //Iniciar variables
        extra = getIntent().getExtras();
        tv_driver_name = findViewById(R.id.tv_driver);
        tv_matricula = findViewById(R.id.tv_matricula);
        tv_time = findViewById(R.id.tv_time2);
        tv_distance = findViewById(R.id.tv_distance2);
        btn_cancel = findViewById(R.id.btn_cancel_trip);
        ctx=this; a = this;


        //Iniciando firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        ref_trip = database.getReference("Trips").child(mUser.getUid()).child(extra.getString("trip_key"));
        ref_user = database.getReference("Users").child("clients").child(mUser.getUid());

        //Iniciando mapa
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_trip);
        mapFragment.getMapAsync(this);

        //Colocando listener para estado del viaje

        ref_trip.child("state").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String trip_state = snapshot.getValue(String.class);

                    if(trip_state.equals("canceled")){
                        ref_user.child("trip").setValue("null");
                        Intent i = new Intent(TripActivity.this, HomeActivity.class);
                        startActivity(i);
                        finish();
                    }
                    if(trip_state.equals("completed")){
                        ref_user.child("trip").setValue("null");
                        showDialogFinished();
                    }
                    if(trip_state.equals("driving")){
                        map.clear();
                        driverLoca=null;
                        LatLng start = new LatLng(extra.getDouble("start_lat"), extra.getDouble("start_long"));
                        LatLng end = new LatLng(extra.getDouble("end_lat"), extra.getDouble("end_long"));
                        findRoutes.findroutes(start,end);
                    }
                    if(trip_state.equals("comming")){
                        driverLoca=null;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //Colocando datos del conductor

        setDriverData(extra.getString("driver"));

        //Metodos Onclick
        setOnClickMethods();
    }

    private void setOnClickMethods() {
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ref_trip.child("state").setValue("canceled");
            }
        });
    }

    private void setDriverData(String driver) {
        DatabaseReference ref_driver = database.getReference("Users").child("drivers").child(driver);
        ref_driver.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String name = snapshot.getValue(String.class);
                    Log.e("msg", name);
                    tv_driver_name.setText(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        ref_driver.child("matricula").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String matricula = snapshot.getValue(String.class);
                    tv_matricula.setText(matricula);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        getDriverLocation(driver);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        beginDriverLocation(extra.getString("driver"));
    }

    private void beginDriverLocation(String id_driver) {
        DatabaseReference ref_driver = database.getReference("Users").child("drivers").child(id_driver).child("location");
        ref_driver.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String driverlocation = dataSnapshot.getValue(String.class);
                    if(!driverlocation.isEmpty()){
                        String [] loca = driverlocation.split("#");
                        String latitude = loca[0];
                        String longitude = loca[1];
                        Double[] driverLocation = new Double[]{Double.parseDouble(latitude), Double.parseDouble(longitude)};
                        if(!latitude.isEmpty() && !longitude.isEmpty()) {
                            if(driverLoca ==null){
                                driverLoca = new LatLng(driverLocation[0], driverLocation[1]);
                                //marcador conductor
                                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_car2);
                                MarkerOptions marker = new MarkerOptions().position(driverLoca);
                                marker.icon(icon);
                                m = map.addMarker(marker);
                                map.moveCamera(CameraUpdateFactory.newLatLng(driverLoca));
                                Toast.makeText(TripActivity.this, driverlocation, Toast.LENGTH_SHORT).show();
                            }
                            driverLoca = new LatLng(driverLocation[0], driverLocation[1]);
                            m.setPosition(driverLoca);

                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getDriverLocation(String idDriver){
        DatabaseReference ref_driver = database.getReference("Users").child("drivers").child(idDriver).child("location");
        ref_driver.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String driverLocation = snapshot.getValue(String.class);
                    String [] loca = driverLocation.split("#");
                    String latitude = loca[0];
                    String longitude = loca[1];
                    LatLng start = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                    LatLng end = new LatLng(extra.getDouble("start_lat"), extra.getDouble("start_long"));

                    findRoutes = new FindRoutes(ctx,a,map,polylines, start, end);
                    findRoutes.findroutes(start, end);
                    new MyAsyncTask().execute();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showDialogFinished(){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(TripActivity.this);
        View v = getLayoutInflater().inflate(R.layout.dialog_finished,null);

        Button btn_done = v.findViewById(R.id.btn_done);
        TextView tv_finished = v.findViewById(R.id.tv_trip_finished);

        mBuilder.setCancelable(false);
        mBuilder.setView(v);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        tv_finished.setText("Pague al conductor.");
        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent i = new Intent(TripActivity.this, HomeActivity.class);
                startActivity(i);
                finish();
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
            tv_distance.setText(findRoutes.getDistance());
            tv_time.setText(findRoutes.getTime());
        }
    }

    @Override
    protected void onDestroy() {
        ref_trip.child("state").setValue("canceled");
        ref_user.child("trip").setValue("null");
        super.onDestroy();
    }
}
