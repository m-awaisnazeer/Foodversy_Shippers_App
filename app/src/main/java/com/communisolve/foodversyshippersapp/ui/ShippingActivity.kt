package com.communisolve.foodversyshippersapp.ui

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.communisolve.foodversyshippersapp.R
import com.communisolve.foodversyshippersapp.common.Common
import com.communisolve.foodversyshippersapp.databinding.ActivityShippingBinding
import com.communisolve.foodversyshippersapp.model.ShippingOrderModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.paperdb.Paper
import java.text.SimpleDateFormat

class ShippingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityShippingBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var shipperMarker: Marker? = null
    private var shippingOrderModel: ShippingOrderModel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShippingBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

                    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@ShippingActivity)
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,
                        Looper.getMainLooper())
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
            }).check()
    }

    private fun setShippingOrderModel() {
        Paper.init(this)
        val data = Paper.book().read<String>(Common.SHIPPING_DATA)
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
                } else {
                    shipperMarker!!.position = locationShipper
                }

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 15f))
            }

        }
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
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,
            R.raw.uber_light_with_label))
            if (!success){

            }
        }catch (ex:Resources.NotFoundException){

        }
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}