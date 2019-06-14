package co.com.prueba

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import co.com.appmovil.Broadcast.GpsReceiver
import co.com.prueba.Model.Trips
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.Serializable
import java.text.DecimalFormat
import kotlin.random.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener, View.OnClickListener {

    var arrayCity = ArrayList<Trips>()

    private var mGoogleMap: GoogleMap? = null

    private var mGoogleApiClient: GoogleApiClient? = null

    private var mLocationRequest: LocationRequest? = null

    private var alertDialogBuilder: AlertDialog.Builder? = null

    private var alertDialog: AlertDialog? = null

    private val ACCESS_FINE_LOCATION = 101

    private val ACCESS_COARSE_LOCATION = 102

    private var myLatLng: LatLng? = null

    private var sm: SensorManager? = null

    private var sensorAccelerometer: Sensor? = null

    private var sensorGyroscope: Sensor? = null

    private var arrayMarker = ArrayList<Marker>()

    private var polylineOne: Polyline? = null

    private var polylineTwo: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.fMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        accessFineLocation()

        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAccelerometer = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorGyroscope = sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val json = JSONObject(loadJSONFromAsset())
        val arrayString = json.getJSONArray("trips").toString()

        val array = arrayFromJson(arrayString, Trips::class.java) as ArrayList<Trips>

        for (trip in array) {
            val start = trip.start.pickup_location.coordinates
            val end = trip.end.pickup_location.coordinates

            val startLatitude = start[1]
            val startLongitude = start[0]
            val endLatitude = end[1]
            val endLongitude = end[0]
            if (startLatitude >= 6.0 && startLatitude < 7.0 &&
                endLatitude >= 6.0 && endLatitude < 7.0 &&
                startLongitude <= -75.0 && endLongitude <= -75.0
            ) arrayCity.add(trip)
        }
    }

    private fun loadJSONFromAsset(): String? {
        val jsonFile: String?
        try {
            val file = assets.open("trips.json")
            val size = file.available()
            val buffer = ByteArray(size)
            file.read(buffer)
            file.close()
            jsonFile = String(buffer)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        return jsonFile
    }

    private fun objectFromJson(json: String, type: Class<out Serializable>): Serializable? {
        var model: Serializable? = null
        try {
            model = Gson().fromJson(json, type)
        } catch (e: JsonSyntaxException) {
            Log.e("Gson error in", type.toString())
            e.printStackTrace()
        }

        return model
    }

    private fun arrayFromJson(json: String, type: Class<out Serializable>): java.util.ArrayList<out Serializable?> {
        val jsonArray = JSONArray(json)
        return (0 until jsonArray.length())
            .map { jsonArray.get(it).toString() }
            .mapTo(java.util.ArrayList()) { objectFromJson(it, type) }
    }

    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient!!.connect()
    }

    private fun accessFineLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val accessFineLocation =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            if (accessFineLocation != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), ACCESS_FINE_LOCATION)
        }
    }

    private fun accessCoarseLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val accessCoarseLocation =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), ACCESS_COARSE_LOCATION)
        }
    }

    private fun checkGps() {
        val isEnabled = GpsReceiver().isEnabled()
        if (!isEnabled) {
            alertDialogBuilder = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
            alertDialogBuilder!!.setTitle(getString(R.string.error_gps))
            alertDialogBuilder!!.setMessage(getString(R.string.no_gps))
            alertDialogBuilder!!.setCancelable(false)
            alertDialogBuilder!!.setPositiveButton(getString(R.string.habilitar)) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            alertDialog = alertDialogBuilder!!.create()
            alertDialog!!.show()
        }
    }

    private fun calculationByDistance(StartP: LatLng, EndP: LatLng): Int {
        val radius = 6371// radio de la tierra en  kilómetros
        val lat1 = StartP.latitude
        val lat2 = EndP.latitude
        val lon1 = StartP.longitude
        val lon2 = EndP.longitude
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + (Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2))
        val c = 2 * Math.asin(Math.sqrt(a))
        val valueResult = radius * c
        val km = valueResult / 1
        val newFormat = DecimalFormat("####")
        val kmInDec = Integer.valueOf(newFormat.format(km))
        val meter = km * 1000
        val meterInDec = Integer.valueOf(newFormat.format(meter))
        Log.i(
            "Radius Value", "" + valueResult + "   KM  " + kmInDec
                    + " Meter   " + meterInDec
        )
        return meterInDec
    }

    private fun orderPoints(
        start: ArrayList<Int>,
        latLngStart: ArrayList<LatLng>,
        latLngEnd: ArrayList<LatLng>
    ): ArrayList<Int> {
        var iOrder = 0
        var jOrder: Int
        while (iOrder < start.size - 1) {
            jOrder = 0
            while (jOrder < start.size - iOrder - 1) {
                if (start[jOrder + 1] < start[jOrder]) {

                    val auxStart = start[jOrder + 1]
                    start[jOrder + 1] = start[jOrder]
                    start[jOrder] = auxStart

                    val auxLatLngStart = latLngStart[jOrder + 1]
                    latLngStart[jOrder + 1] = latLngStart[jOrder]
                    latLngStart[jOrder] = auxLatLngStart

                    val auxLatLngEnd = latLngEnd[jOrder + 1]
                    latLngEnd[jOrder + 1] = latLngEnd[jOrder]
                    latLngEnd[jOrder] = auxLatLngEnd
                }
                jOrder++
            }
            iOrder++
            if (iOrder == (start.size - 1)) {
                val polylineOptionsOne = PolylineOptions()
                polylineOptionsOne.color(Color.BLUE)
                polylineOptionsOne.width(10f)
                polylineOptionsOne.add(latLngStart[0])
                polylineOptionsOne.add(latLngEnd[0])
                polylineOne = mGoogleMap?.addPolyline(polylineOptionsOne)

                val polylineOptionsTwo = PolylineOptions()
                polylineOptionsTwo.color(Color.BLUE)
                polylineOptionsTwo.width(10f)
                polylineOptionsTwo.add(latLngStart[1])
                polylineOptionsTwo.add(latLngEnd[1])
                polylineTwo = mGoogleMap?.addPolyline(polylineOptionsTwo)
            }
        }
        return start
    }

    private fun addPoints() {
        val random = Random
        val arrayDistanceStart = ArrayList<Int>()
        val arrayLatLngStart = ArrayList<LatLng>()
        val arrayLatLngEnd = ArrayList<LatLng>()
        for (i in 0..4) {
            val number = random.nextInt(arrayCity.size)
            val start = LatLng(
                arrayCity[number].start.pickup_location.coordinates[1],
                arrayCity[number].start.pickup_location.coordinates[0]
            )
            val end = LatLng(
                arrayCity[number].end.pickup_location.coordinates[1],
                arrayCity[number].end.pickup_location.coordinates[0]
            )
            val distanceStart = calculationByDistance(myLatLng!!, start)
            arrayDistanceStart.add(distanceStart)
            arrayLatLngStart.add(start)
            arrayLatLngEnd.add(end)
            val markerOne = mGoogleMap?.addMarker(MarkerOptions().position(start).title("Viaje $i comienzo"))
            markerOne?.tag = number
            markerOne?.showInfoWindow()
            arrayMarker.add(markerOne!!)
            val markerTwo = mGoogleMap?.addMarker(MarkerOptions().position(end).title("Viaje $i final"))
            markerTwo?.tag = number
            markerTwo?.showInfoWindow()
            arrayMarker.add(markerTwo!!)
            if (i == 4)
                orderPoints(arrayDistanceStart, arrayLatLngStart, arrayLatLngEnd)
        }
        mGoogleMap?.setOnMarkerClickListener { m ->
            m.showInfoWindow()
            for (marker in arrayMarker)
                if (marker.tag != m.tag)
                    marker.remove()
            polylineOne?.remove()
            polylineTwo?.remove()
            arrayMarker.clear()

            val position = m.tag.toString().toInt()
            val trip = arrayCity[position]

            val start = LatLng(
                trip.start.pickup_location.coordinates[1],
                trip.start.pickup_location.coordinates[0]
            )
            val end = LatLng(
                trip.end.pickup_location.coordinates[1],
                trip.end.pickup_location.coordinates[0]
            )

            //
            val p1 = mGoogleMap?.projection
            val mSP1 = p1?.toScreenLocation(start)
            val pHSF1 =
                Point(
                    mSP1?.x!!,
                    (mSP1.y + (resources.displayMetrics.heightPixels / 2))
                )
            val mLatLng1 = p1.fromScreenLocation(pHSF1)

            //
            val p2 = mGoogleMap?.projection
            val mSP2 = p2?.toScreenLocation(end)
            val pHSF2 =
                Point(
                    mSP2?.x!!,
                    (mSP2.y + (resources.displayMetrics.heightPixels / 2))
                )
            val mLatLng2 = p2.fromScreenLocation(pHSF2)

            val builderPositions = LatLngBounds.Builder()
            builderPositions.include(mLatLng1)
            builderPositions.include(mLatLng2)

            val cu = CameraUpdateFactory.newLatLngBounds(builderPositions.build(), 500)
            mGoogleMap?.animateCamera(cu)

            clViewSummaryRoute.visibility = View.VISIBLE
            tvStart.text = trip.start.pickup_address
            tvEnd.text = trip.end.pickup_address
            true
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            btViewSummary -> {
                mGoogleMap?.clear()
                addPoints()
                clViewSummaryRoute.visibility = View.GONE
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ACCESS_FINE_LOCATION ->
                if (grantResults[0] == PackageManager.PERMISSION_DENIED)
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) accessFineLocation()
                    else return
                else if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    accessCoarseLocation()
            ACCESS_COARSE_LOCATION ->
                if (grantResults[0] == PackageManager.PERMISSION_DENIED)
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    ) accessCoarseLocation()
                    else if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        checkGps()
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mGoogleMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mGoogleMap?.isMyLocationEnabled = true
            buildGoogleApiClient()
        }
        checkGps()
        mGoogleMap?.setOnMapLoadedCallback {
            addPoints()
        }
    }

    override fun onConnected(p0: Bundle?) {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = 3000
        mLocationRequest!!.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
    }

    override fun onConnectionSuspended(p0: Int) {}

    override fun onConnectionFailed(p0: ConnectionResult) {}

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            myLatLng = LatLng(location.latitude, location.longitude)
            val camera = CameraPosition.Builder()
                .target(myLatLng)
                .zoom(11f)
                .bearing(location.bearing)
                .tilt(0f)
                .build()
            mGoogleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
            if (mGoogleApiClient != null) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
            }
        }
    }
}
