package com.communisolve.foodversyshippersapp.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.communisolve.foodversyshippersapp.callbacks.IsShippingOrderCallbackListner
import com.communisolve.foodversyshippersapp.common.Common
import com.communisolve.foodversyshippersapp.model.ShippingOrderModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel(), IsShippingOrderCallbackListner {

    private val orderModelMutableLiveData: MutableLiveData<List<ShippingOrderModel>>
    val messageError: MutableLiveData<String>
    private val listener: IsShippingOrderCallbackListner

    init {
        orderModelMutableLiveData = MutableLiveData()
        messageError = MutableLiveData()
        listener = this
    }
    fun getOrderModelMutableLiveData(shipperPhone: String): MutableLiveData<List<ShippingOrderModel>> {
        loadOrderByShipper(shipperPhone)
        return orderModelMutableLiveData
    }

    private fun loadOrderByShipper(shipperPhone: String) {
        val tempList :MutableList<ShippingOrderModel> = ArrayList()
        val orderRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPINGORDER_REF)
            .orderByChild("shipperPhone")
            .equalTo(Common.currentShipperUser!!.phone)

        orderRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    for (shippingOrderSnapshot in snapshot.children){
                        val orderItem = shippingOrderSnapshot.getValue(ShippingOrderModel::class.java)
                        tempList.add(orderItem!!)
                    }
                    listener.onShippingOrderLoadSuccess(tempList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onShippingOrderLoadFailed(error.message)
            }

        })

    }

    override fun onShippingOrderLoadSuccess(shippingOrders: List<ShippingOrderModel>) {
        orderModelMutableLiveData.value = shippingOrders
    }

    override fun onShippingOrderLoadFailed(message: String) {
        messageError.value = message
    }
}