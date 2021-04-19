package com.communisolve.foodversyshippersapp.callbacks

import com.communisolve.foodversyshippersapp.model.ShippingOrderModel

interface IsShippingOrderCallbackListner {
    fun onShippingOrderLoadSuccess(shippingOrders: List<ShippingOrderModel>)
    fun onShippingOrderLoadFailed(message:String)

}