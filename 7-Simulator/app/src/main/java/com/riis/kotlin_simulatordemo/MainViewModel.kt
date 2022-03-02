package com.riis.kotlin_simulatordemo

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

class MainViewModel: ViewModel(), DJISDKManager.SDKManagerCallback {

    var product: BaseProduct? = null

    val connectionStatus: MutableLiveData<Boolean> = MutableLiveData(false)

    companion object {
        const val TAG = "MainViewModel"
    }

    fun startSdkRegistration(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            DJISDKManager.getInstance().registerApp(context, this@MainViewModel)
        }
    }

    fun getFlightController(): FlightController? {
        return (product as Aircraft?)?.flightController
    }

    override fun onRegister(error: DJIError?) {
        if(error == DJISDKError.REGISTRATION_SUCCESS){
            Log.i(TAG, "Application registered")
            DJISDKManager.getInstance().startConnectionToProduct()
        }else{
            Log.e(TAG, "Registration failed: ${error?.description}")
        }
    }
    override fun onProductDisconnect() {
        Log.d(TAG, "onProductDisconnect")
        product = null
        connectionStatus.postValue(false)
    }
    override fun onProductConnect(baseProduct: BaseProduct?) {
        Log.d(TAG, "onProductConnect newProduct: $baseProduct")
        product = baseProduct
        connectionStatus.postValue(product?.isConnected)
    }

    override fun onComponentChange(
        componentKey: BaseProduct.ComponentKey?,
        oldComponent: BaseComponent?,
        newComponent: BaseComponent?
    ) {
        newComponent?.setComponentListener { isConnected ->
            Log.d(TAG, "onComponentConnectivityChanged: $isConnected")
        }
        Log.d(TAG, "onComponentChange key: $componentKey, oldComponent: $oldComponent, newComponent: $newComponent")
    }

    override fun onProductChanged(baseProduct: BaseProduct?) {
        try {
            product = baseProduct
            connectionStatus.postValue(product?.isConnected)
        } catch (E: Exception) {
            Log.e(TAG, "Product is not an aircraft")
        }
    }
    
    override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent?, i: Int) {}
    override fun onDatabaseDownloadProgress(l: Long, l1: Long) {}
    
}