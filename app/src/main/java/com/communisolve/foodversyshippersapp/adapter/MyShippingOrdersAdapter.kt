package com.communisolve.foodversyshippersapp.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.communisolve.foodversyshippersapp.common.Common
import com.communisolve.foodversyshippersapp.databinding.LayoutShippingOrderBinding
import com.communisolve.foodversyshippersapp.model.ShipperUserModel
import com.communisolve.foodversyshippersapp.model.ShippingOrderModel
import com.communisolve.foodversyshippersapp.ui.ShippingActivity
import com.google.gson.Gson
import io.paperdb.Paper
import java.text.SimpleDateFormat

class MyShippingOrdersAdapter(
    var context: Context,
    var shippingOrderModelList:List<ShippingOrderModel>
) : RecyclerView.Adapter<MyShippingOrdersAdapter.ViewHolder>() {

    var simpleDateFormat:SimpleDateFormat

    init {
        simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        Paper.init(context)
    }

    private lateinit var binding: LayoutShippingOrderBinding

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding =
            LayoutShippingOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var shippingOrder = shippingOrderModelList[position]
        Glide.with(context).load(shippingOrder.orderModel!!.cartItemList!![0].foodImage).into(binding.imgFood)
        Common.setSpanStringColor("No.: ",shippingOrder.orderModel!!.key,binding.txt0rderNumber,
            Color.parseColor("#BA454A"))

        Common.setSpanStringColor("Address: ",shippingOrder.orderModel!!.shippingAddress,binding.txt0rderAddress,
            Color.parseColor("#BA454A"))

        Common.setSpanStringColor("Payment: ",shippingOrder.orderModel!!.transactionId,binding.txtPayment,
            Color.parseColor("#BA454A"))

        if (shippingOrder.isStartTrip){
            binding.btnShipNow.isEnabled = false
        }

        //Event
        binding.btnShipNow.setOnClickListener {
          //Write Data
            Paper.book().write(Common.SHIPPING_DATA,Gson().toJson(shippingOrderModelList[0]))

            context.startActivity(Intent(context,ShippingActivity::class.java))
        }

    }

    override fun getItemCount(): Int = shippingOrderModelList.size
}