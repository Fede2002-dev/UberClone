package uberapp.balran.uberapp.utilities

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import com.directions.route.*
import com.example.uberapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import java.util.*

class FindRoutes(ctx: Context, activity: Activity, val map: GoogleMap, polylines: MutableList<Polyline>?, start: LatLng, end: LatLng) : RoutingListener {
    private val activity: Activity
    private var polylines: MutableList<Polyline>? = null
    private val start: LatLng
    private val ctx: Context
    private val end: LatLng
    var polyOptions: PolylineOptions? = null
        private set
    var distance: String? = null
        private set
    var time: String? = null
        private set
    var isFinded = false
        private set


    // function to find Routes.
    fun findroutes(Start: LatLng?, End: LatLng?) {
        if (Start == null || End == null) {
            //Toast.makeText(ctx,"Unable to get location",Toast.LENGTH_LONG).show();
        } else {
            val routing = Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(Start, End)
                    .key("AIzaSyC9tRPBnPxGm1EkNcRWvAv0YGbcj9j5Pkw") //also define your api key here.
                    .build()
            routing.execute()
        }
    }

    //Routing call back functions.
    override fun onRoutingFailure(e: RouteException) {
        isFinded = false
        Toast.makeText(ctx, "Error al obtener ruta.", Toast.LENGTH_SHORT).show()
        val parentLayout = activity.findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_LONG)
        snackbar.show()
        findroutes(start, end)
    }

    override fun onRoutingStart() {
        isFinded = false
    }

    //If Route finding success..
    override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
        val center = CameraUpdateFactory.newLatLng(start)
        val zoom = CameraUpdateFactory.zoomTo(16f)
        if (polylines != null) {
            polylines!!.clear()
        }
        polyOptions = PolylineOptions()
        var polylineStartLatLng: LatLng? = null
        var polylineEndLatLng: LatLng? = null
        polylines = ArrayList()
        //add route(s) to the map using polyline
        for (i in route.indices) {
            if (i == shortestRouteIndex) {
                distance = route[shortestRouteIndex].distanceText
                time = route[shortestRouteIndex].durationText
                polyOptions!!.color(activity.resources.getColor(R.color.colorPrimary))
                polyOptions!!.width(7f)
                polyOptions!!.addAll(route[shortestRouteIndex].points)
                val polyline = map.addPolyline(polyOptions)
                polylineStartLatLng = polyline.points[0]
                val k = polyline.points.size
                polylineEndLatLng = polyline.points[k - 1]
                polylines!!.add(polyline)
                isFinded = true
            } else {
            }
        }

        //Add Marker on route ending position
        val endMarker = MarkerOptions()
        endMarker.position(polylineEndLatLng!!)
        endMarker.title("Destino")
        endMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination))
        map.addMarker(endMarker)

        //Moving camera to end pos
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 16f))
    }

    override fun onRoutingCancelled() {
        isFinded = false
        findroutes(start, end)
    }

    val isFind: Boolean
        get() {
            while (isFinded != true) {
            }
            return isFinded
        }

    init {
        this.polylines = polylines
        this.start = start
        this.ctx = ctx
        this.activity = activity
        this.end = end
    }
}