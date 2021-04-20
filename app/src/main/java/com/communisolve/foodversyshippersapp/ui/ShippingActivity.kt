package com.communisolve.foodversyshippersapp.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.communisolve.foodversyshippersapp.R
import com.communisolve.foodversyshippersapp.common.Common
import com.communisolve.foodversyshippersapp.databinding.ActivityShippingBinding
import com.communisolve.foodversyshippersapp.model.ShippingOrderModel
import com.communisolve.foodversyshippersapp.remote.IGoogleApi
import com.communisolve.foodversyshippersapp.remote.RetrofitClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.paperdb.Paper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ShippingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityShippingBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var shipperMarker: Marker? = null
    private var shippingOrderModel: ShippingOrderModel? = null


    var isInit = false
    var previousLocation: Location? = null


    private var handler: Handler? = null
    private var index: Int = -1
    private var next: Int = 0
    private var startPosition: LatLng? = LatLng(0.0, 0.0)
    private var endPosition: LatLng? = LatLng(0.0, 0.0)
    private var v: Float = 0f
    private var lat: Double = -1.0
    private var lng: Double = -1.0

    private var blackPolyLines: Polyline? = null
    private var greyPolyLines: Polyline? = null

    private var polyLineOptions: PolylineOptions? = null
    private var blackPolyLinesOptions: PolylineOptions? = null

    private var polylineList: List<LatLng> = ArrayList()
    private var iGoogleApi: IGoogleApi? = null
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private lateinit var places_fragment: AutocompleteSupportFragment
    private lateinit var placesClient: PlacesClient
    private val placesFields = Arrays.asList(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShippingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iGoogleApi = RetrofitClient.instance!!.create(IGoogleApi::class.java)
        initPlaces()
        setupPlacesAutoComplete()
        buildLocationRequest()
        buildLocationCallback()

        setShippingOrderModel()

        Dexter.withActivity(this)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                @SuppressLint("MissingPermission")
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val mapFragment = supportFragmentManager
                        .findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this@ShippingActivity)

                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(this@ShippingActivity)
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest, locationCallback,
                        Looper.getMainLooper()
                    )
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        this@ShippingActivity,
                        "you need permission to use this app",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }
            })
            .check()

        initViews()
    }



    private fun setupPlacesAutoComplete() {
        supportFragmentManager
        places_fragment = supportFragmentManager.findFragmentById(R.id.places_auto_complete_fragment) as AutocompleteSupportFragment
        places_fragment.setPlaceFields(placesFields)
        places_fragment.setOnPlaceSelectedListener(object :PlaceSelectionListener{
            override fun onPlaceSelected(p0: Place) {
                Toast.makeText(this@ShippingActivity, java.lang.StringBuilder(p0.name)
                    .append("-")
                    .append(p0.latLng).toString(), Toast.LENGTH_SHORT).show()
            }

            override fun onError(p0: Status) {
                Toast.makeText(this@ShippingActivity, "${p0.statusMessage}", Toast.LENGTH_SHORT)
                    .show()
            }

        })
    }

    private fun initPlaces() {
        Places.initialize(this,getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

    }

    private fun initViews() {
        binding.btnStartTrip.setOnClickListener {
            val data = Paper.book().read<String>(Common.SHIPPING_DATA)
            Paper.book().write(Common.TRIP_DATA, data)
            binding.btnStartTrip.isEnabled = false
        }
    }

    private fun setShippingOrderModel() {
        Paper.init(this)
        var data = ""
        if (TextUtils.isEmpty(Paper.book().read(Common.TRIP_DATA))) {
            data = Paper.book().read<String>(Common.SHIPPING_DATA)
            binding.btnStartTrip.isEnabled = true
        } else {
            data = Paper.book().read<String>(Common.TRIP_DATA)
            binding.btnStartTrip.isEnabled = false
        }
        if (!TextUtils.isEmpty(data)) {
            shippingOrderModel = Gson().fromJson<ShippingOrderModel>(
                data,
                object : TypeToken<ShippingOrderModel>() {}.type
            )

            if (shippingOrderModel != null) {
                Common.setSpanStringColor(
                    "Name: ",
                    shippingOrderModel!!.orderModel!!.userName,
                    binding.txtName,
                    Color.parseColor("#333639")
                )

                Common.setSpanStringColor(
                    "Address: ",
                    shippingOrderModel!!.orderModel!!.shippingAddress,
                    binding.txtAddress,
                    Color.parseColor("#673ab7")
                )

                Common.setSpanStringColor(
                    "No.: ",
                    shippingOrderModel!!.orderModel!!.key,
                    binding.txtOrderNumber,
                    Color.parseColor("#795548")
                )

                binding.txtDate.text = StringBuilder().append(
                    SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(shippingOrderModel!!.orderModel!!.createDate)
                )

                Glide.with(this)
                    .load(shippingOrderModel!!.orderModel!!.cartItemList!![0].foodImage)
                    .into(binding.imgFoodImage)
            }
        } else {
            Toast.makeText(this, "Shipping Model Null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val locationShipper = LatLng(
                    locationResult.lastLocation.latitude,
                    locationResult.lastLocation.longitude
                )

                if (shipperMarker == null) {
                    val height = 80
                    val width = 80
                    val bitmapDrawable = ContextCompat.getDrawable(
                        this@ShippingActivity,
                        R.drawable.shipper
                    )
                    val b = bitmapDrawable!!.toBitmap()
                    val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
                    shipperMarker = mMap.addMarker(
                        MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker!!))
                            .position(locationShipper)
                            .title("You")
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18f))

                }

//                else {
//                    shipperMarker!!.position = locationShipper
//                }

                if (isInit && previousLocation != null) {

                    val from = java.lang.StringBuilder()
                        .append(previousLocation!!.latitude)
                        .append(",")
                        .append(previousLocation!!.longitude)

                    val to = java.lang.StringBuilder()
                        .append(locationShipper!!.latitude)
                        .append(",")
                        .append(locationShipper!!.longitude)

                    moveMarkerAnimation(shipperMarker, from, to)
//                    val previousLocationLatlng =
//                        LatLng(previousLocation!!.latitude, previousLocation!!.longitude)
//                    MarkerAnimation.animateMarkerToGB(
//                        shipperMarker!!,
//                        locationShipper,
//                        LatLngInterpolator.Spherical()
//                    )
//                    shipperMarker!!.rotation =
//                        Common.getBearing(previousLocationLatlng, locationShipper)
//                    mMap.animateCamera(CameraUpdateFactory.newLatLng(locationShipper))

                    previousLocation = locationResult.lastLocation
                }

                if (!isInit) {
                    isInit = true
                    previousLocation = locationResult.lastLocation
                }

            }

        }
    }

    private fun moveMarkerAnimation(
        marker
        : Marker?,
        from: java.lang.StringBuilder,
        to: java.lang.StringBuilder
    ) {
        compositeDisposable.addAll(
            iGoogleApi!!.getDirections(
                "driving",
                "less_driving",
                from.toString(),
                to.toString(),
                getString(R.string.google_maps_key)
            )!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    try {
                        val jsonObject = JSONObject(it)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polyLineOptions = PolylineOptions()
                        polyLineOptions!!.color(Color.GRAY)
                        polyLineOptions!!.width(5f)
                        polyLineOptions!!.startCap(SquareCap())
                        polyLineOptions!!.endCap(SquareCap())
                        polyLineOptions!!.jointType(JointType.ROUND)
                        polyLineOptions!!.addAll(polylineList)

                        greyPolyLines = mMap.addPolyline(polyLineOptions)



                        blackPolyLinesOptions = PolylineOptions()
                        blackPolyLinesOptions!!.color(Color.BLACK)
                        blackPolyLinesOptions!!.width(5f)
                        blackPolyLinesOptions!!.startCap(SquareCap())
                        blackPolyLinesOptions!!.endCap(SquareCap())
                        blackPolyLinesOptions!!.jointType(JointType.ROUND)
                        blackPolyLinesOptions!!.addAll(polylineList)
                        blackPolyLines = mMap.addPolyline(blackPolyLinesOptions)

                        //Animator
                        val polylineAnimator = ValueAnimator.ofInt(0, 100)
                        polylineAnimator.setDuration(2000L)
                        polylineAnimator.interpolator = (LinearInterpolator())
                        polylineAnimator.addUpdateListener { valueAnimator ->
                            val points = greyPolyLines!!.points
                            var percentValue =
                                Integer.parseInt(valueAnimator.animatedValue.toString())
                            var size: Int = points.size
                            val newPoints = (size * (percentValue / 100.0f)).toInt()
                            val p = points.subList(0, newPoints)
                            blackPolyLines!!.points = p
                        }

                        polylineAnimator.start()

                        //Car Moving
                        index = -1
                        next = 1
                        val r = object : Runnable {
                            override fun run() {
                                if (index < polylineList.size - 1) {
                                    index++
                                    next = index + 1
                                    startPosition = polylineList[index]
                                    endPosition = polylineList[next]
                                }
                                val valueAnimator = ValueAnimator.ofInt(0, 1)
                                valueAnimator.setDuration(1500)
                                valueAnimator.interpolator = LinearInterpolator()
                                valueAnimator.addUpdateListener { valueAnimator ->
                                    v = valueAnimator.animatedFraction
                                    lat =
                                        v * endPosition!!.latitude + (1 - v) * startPosition!!.latitude
                                    lng =
                                        v * endPosition!!.longitude + (1 - v) * startPosition!!.longitude

                                    val newPos = LatLng(lat, lng)
                                    marker!!.position = newPos
                                    marker.setAnchor(0.5f, 0.5f)
                                    marker.rotation = Common.getBearing(startPosition!!, newPos)

                                    mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                                }

                                valueAnimator.start()
                                if (index < polylineList.size - 2)
                                    handler!!.postDelayed(this, 1500)
                            }

                        }

                        handler = Handler()
                        handler!!.postDelayed(r, 1500)


                    } catch (e: Exception) {
                        Log.d("DEBUG", "moveMarkerAnimation: ${e.message}")
                    }
                }, {
                    Toast.makeText(this, "${it.message}", Toast.LENGTH_SHORT).show()
                })
        )
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.setInterval(15000)
        locationRequest.setFastestInterval(10000)
        locationRequest.setSmallestDisplacement(20f)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.uber_light_with_label
                )
            )
            if (!success) {

            }
        } catch (ex: Resources.NotFoundException) {

        }
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        compositeDisposable.clear()
        super.onDestroy()
    }
}