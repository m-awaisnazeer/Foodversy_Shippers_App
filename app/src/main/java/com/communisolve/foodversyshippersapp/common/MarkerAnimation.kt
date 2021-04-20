package com.communisolve.foodversyshippersapp.common

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class MarkerAnimation {

    companion object{
        fun animateMarkerToGB(marker:Marker,finalPosotion:LatLng,
        latLngInterpolator: LatLngInterpolator){
            val startPosition = marker.position
            var handler = Handler()
            var start = SystemClock.uptimeMillis()
            val interpolator = AccelerateDecelerateInterpolator()
            val durationinMs = 3000

            handler.post(object :Runnable{
                var elapsed=0L
                var t=0L
                var v = 0f
                override fun run() {
                    elapsed = SystemClock.uptimeMillis()-start
                    t = elapsed/durationinMs
                    v = interpolator.getInterpolation(t.toFloat())

                    marker.position = latLngInterpolator.interpolate(v,startPosition,finalPosotion)

                    //Repeat till progress is complete

                    if (t<1){
                        handler.postDelayed(this,16)
                    }
                }

            })
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun animateMarkertoHC(marker: Marker,finalPosotion: LatLng,latLngInterpolator: LatLngInterpolator)
        {
            val startPosition = marker.position

            val valueAnimator = ValueAnimator()
            valueAnimator.addUpdateListener {
                var v = it.animatedFraction
                var newPosition = latLngInterpolator.interpolate(v,startPosition,finalPosotion)
                marker.position = newPosition
            }

            valueAnimator.setFloatValues(0f,1f)
            valueAnimator.duration = 3000
            valueAnimator.start()
        }



    }
}