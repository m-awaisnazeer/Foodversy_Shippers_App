package com.communisolve.foodversyshippersapp.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.communisolve.foodversyshippersapp.R
import com.communisolve.foodversyshippersapp.model.ShipperUserModel
import com.communisolve.foodversyshippersapp.model.TokenModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase

object Common {

    fun setSpanStringColor(welcome: String, name: String?, txtTime: TextView, parseColor: Int) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val txtSpanable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpanable.setSpan(boldSpan, 0, name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        txtSpanable.setSpan(
            ForegroundColorSpan(parseColor),
            0,
            name!!.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(txtSpanable)
        txtTime.setText(builder, TextView.BufferType.SPANNABLE)
    }

    fun getNewOrderTopic(): String? {
        return java.lang.StringBuilder("/topics/new_order").toString()
    }

    val TRIP_DATA: String?="Trip"
    val SHIPPING_DATA: String? = "ShippingData"
    val SHIPPINGORDER_REF: String = "ShippingOrder"
    val NOTI_CONTENT: String? = "content"
    val NOTI_TITLE: String? = "title"
    val TOKEN_REF: String = "Tokens"
    fun updateToken(
        context: Context,
        token: String,
        isServerToken: Boolean,
        isShipperToken: Boolean
    ) {
        if (currentShipperUser != null)
            FirebaseDatabase.getInstance().getReference(Common.TOKEN_REF)
                .child(Common.currentShipperUser!!.uid)
                .setValue(
                    TokenModel(
                        currentShipperUser!!.uid,
                        token,
                        isServerToken,
                        isShipperToken
                    )
                )
                .addOnFailureListener {
                    Toast.makeText(context, "$token", Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener { }

    }

    fun showNotification(
        context: Context, id: Int, title: String?, content: String?,
        intent: Intent?
    ) {
        var pendingInent: PendingIntent? = null
        if (intent != null)
            pendingInent =
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID = "com.communisolve.foodversyshippersapp"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "Foodversy_Shipper",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationChannel.description = "Foodversy_Shipper_App"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)

        builder.setContentTitle(title).setContentText(content).setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_baseline_restaurant_menu_24
                )
            )
        if (pendingInent != null)
            builder.setContentIntent(pendingInent)

        val notification = builder.build()
        notificationManager.notify(id, notification)
    }

    fun convertStatustoString(orderStatus: Int): String? =
        when (orderStatus) {
            0 -> "Placed"
            1 -> "Shipping"
            2 -> "Shipped"
            -1 -> "Cancelled"
            else -> "Error"
        }

    fun getBearing(begin: LatLng, end: LatLng): Float {
        val lat = Math.abs(begin.latitude - end.longitude)
        val lng = Math.abs(begin.longitude - end.longitude)
        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return Math.toDegrees(Math.atan(lng / lat)).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (90 - Math.toDegrees(Math.atan(lng / lat)) + 90).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (Math.toDegrees(Math.atan(lng / lat)) + 180).toFloat()
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (90 - Math.toDegrees(Math.atan(lng / lat)) + 270).toFloat()
        return -1.0f
    }

    fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList<LatLng>()
        var index = 0
        var len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift+=5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlng
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    val ORDER_REF: String = "Orders"

    val CATEGORY_REF: String = "Category"
    val SHIPPERS_REF = "Shippers"
    var currentShipperUser: ShipperUserModel? = null

    val FULL_WIDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int = 0
}