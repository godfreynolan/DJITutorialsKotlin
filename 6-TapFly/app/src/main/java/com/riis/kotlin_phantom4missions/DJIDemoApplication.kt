package com.riis.kotlin_phantom4missions

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager

class DJIDemoApplication : Application() {

    companion object {
        const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
        val TAG = DJIDemoApplication::class.java.name
        var mProduct: BaseProduct? = null


        @Synchronized
        fun getProductInstance(): BaseProduct? {
            if (mProduct == null) {
                mProduct = DJISDKManager.getInstance().product
            }
            return mProduct
        }

        @Synchronized
        fun getCameraInstance(): Camera? {
            if (getProductInstance() == null)
                return null

            var camera: Camera? = null
            if (getProductInstance() is Aircraft) {
                camera = (getProductInstance() as Aircraft).camera
            }
            return camera
        }
    }


    private lateinit var mDJISDKManager: DJISDKManager.SDKManagerCallback
    lateinit var mHandler: Handler

    private lateinit var instance: Application

    fun setContext(application: Application) {
        instance = application
    }

    override fun getApplicationContext(): Context {
        return instance
    }

    override fun onCreate() {
        super.onCreate()

        mHandler = Handler(Looper.getMainLooper())

        /**
         * When starting SDK services, an instance of interface DJISDKManager.DJISDKManagerCallback will be used to listen to
         * the SDK Registration result and the product changing.
         */
        mDJISDKManager = (object : DJISDKManager.SDKManagerCallback {
            override fun onRegister(error: DJIError?) {
                Log.i("Register", "Registering")
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    val handler = Handler(Looper.getMainLooper())

                    handler.post {
                        Toast.makeText(
                            applicationContext,
                            "Register Success",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    DJISDKManager.getInstance().startConnectionToProduct()
                } else {
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        Log.i("Register", "Register failed")
                        Toast.makeText(
                            applicationContext,
                            "Register sdk fails, check network is available",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                Log.e("TAG", "$error")
            }

            override fun onProductDisconnect() {
                Log.d("TAG", "onProductDisconnect")
                notifyStatusChange()
            }

            override fun onProductConnect(baseProduct: BaseProduct?) {
                Log.d("TAG", "onProductConnect newProduct: $baseProduct")
                notifyStatusChange()
            }

            override fun onComponentChange(
                componentKey: BaseProduct.ComponentKey?,
                oldComponent: BaseComponent?,
                newComponent: BaseComponent?
            ) {
                newComponent?.setComponentListener { isConnected ->
                    Log.d(
                        "TAG",
                        "onComponentConnectivityChanged: $isConnected"
                    )
                }
                Log.d(
                    "TAG",
                    "onComponentChange key: $componentKey, oldComponent: $oldComponent, newComponent: $newComponent"
                )
            }

            override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent?, i: Int) {

            }

            override fun onProductChanged(p0: BaseProduct?) {
                TODO("Not yet implemented")
            }

            override fun onDatabaseDownloadProgress(l: Long, l1: Long) {

            }

        })
    }

    private fun notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable)
        mHandler.postDelayed(updateRunnable, 500)
    }

    private var updateRunnable = (Runnable {
        val intent = Intent(FLAG_CONNECTION_CHANGE)
        applicationContext.sendBroadcast(intent)
    })


}