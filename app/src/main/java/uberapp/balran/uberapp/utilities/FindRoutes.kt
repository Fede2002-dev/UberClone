package uberapp.balran.uberapp.utilities;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.uberapp.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class FindRoutes implements RoutingListener {
    private GoogleMap map;
    private Activity activity;
    private List<Polyline> polylines=null;
    private LatLng start;
    private Context ctx;
    private LatLng end;
    private PolylineOptions polyOptions;
    private String distance;
    private String time;
    private boolean finded=false;


    public FindRoutes(Context ctx,Activity activity, GoogleMap map, List<Polyline> polylines, LatLng start, LatLng end) {
        this.map = map;
        this.polylines = polylines;
        this.start = start;
        this.ctx = ctx;
        this.activity = activity;
        this.end = end;
    }

    public GoogleMap getMap() {
        return map;
    }

    public List<Polyline> getPolylines() {
        return polylines;
    }

    public PolylineOptions getPolyOptions() {
        return polyOptions;
    }

    public String getDistance() {
        return distance;
    }

    public String getTime() {
        return time;
    }

    public boolean isFinded() {
        return finded;
    }

    // function to find Routes.
    public void findroutes(LatLng Start, LatLng End) {
        if(Start==null || End==null) {
            //Toast.makeText(ctx,"Unable to get location",Toast.LENGTH_LONG).show();
        }
        else
        {
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(Start, End)
                    .key("AIzaSyC9tRPBnPxGm1EkNcRWvAv0YGbcj9j5Pkw")  //also define your api key here.
                    .build();
            routing.execute();
        }
    }

    //Routing call back functions.
    @Override
    public void onRoutingFailure(RouteException e) {
        finded = false;
        Toast.makeText(ctx, "Error al obtener ruta.", Toast.LENGTH_SHORT).show();
        View parentLayout = activity.findViewById(android.R.id.content);
        Snackbar snackbar= Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_LONG);
        snackbar.show();
        findroutes(start,end);
    }

    @Override
    public void onRoutingStart() {
        finded = false;
    }

    //If Route finding success..
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
        if(polylines!=null) {
            polylines.clear();
        }
        polyOptions = new PolylineOptions();
        LatLng polylineStartLatLng=null;
        LatLng polylineEndLatLng=null;


        polylines = new ArrayList<>();
        //add route(s) to the map using polyline
        for (int i = 0; i <route.size(); i++) {

            if(i==shortestRouteIndex)
            {
                distance = route.get(shortestRouteIndex).getDistanceText();
                time = route.get(shortestRouteIndex).getDurationText();
                polyOptions.color(activity.getResources().getColor(R.color.colorPrimary));
                polyOptions.width(7);
                polyOptions.addAll(route.get(shortestRouteIndex).getPoints());

                Polyline polyline = map.addPolyline(polyOptions);
                polylineStartLatLng=polyline.getPoints().get(0);
                int k=polyline.getPoints().size();
                polylineEndLatLng=polyline.getPoints().get(k-1);
                polylines.add(polyline);

                finded = true;
            }
            else {

            }

        }

        //Add Marker on route starting position
        /*MarkerOptions startMarker = new MarkerOptions();
        startMarker.position(polylineStartLatLng);
        startMarker.title("Inicio");
        map.addMarker(startMarker);*/

        //Add Marker on route ending position
        MarkerOptions endMarker = new MarkerOptions();
        endMarker.position(polylineEndLatLng);
        endMarker.title("Destino");
        endMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination));
        map.addMarker(endMarker);

        //Moving camera to end pos
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 16));
    }

    @Override
    public void onRoutingCancelled() {
        finded = false;
        findroutes(start,end);
    }

    public boolean isFind(){
        while(isFinded()!=true){}
        return isFinded();
    }

}
