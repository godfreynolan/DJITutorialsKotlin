package com.riis.kotlin_importandactivatesdkinandroidstudio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    /**
     * The following class variables can be called without having to create an instance of the MainActivity class
     */
    companion object {
        private val TAG = "MainActivity"
        private const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
        private lateinit var mProduct: BaseProduct
        private lateinit var mHandler: Handler //this allows you to send and process Message and Runnable objects associated with a thread's MessageQueue

        //array of permission strings defined in the AndroidManifest, which the app requires and may need to request the user for permission to use.
        private val REQUIRED_PERMISSION_LIST: Array<String> = arrayOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
        )
        private const val REQUEST_PERMISSION_CODE = 125 //integer constant used when requesting app permissions
    }

    /**
     * Class Variables
     */
    private val missingPermission = ArrayList<String>()
    private val isRegistrationInProgress = AtomicBoolean(false) //This boolean can be accessed and updated by multiple threads

    /**
     * Creating the Activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity created")

        //If the Android version running on the user's device is at least Android 6 (Marshmallow) or API level 23, check and request permissions.
        //Android versions below 6 automatically grants access to these permissions, which can be dangerous.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkAndRequestPermissions()
        }

        setContentView(R.layout.activity_main) //inflates the activity_main layout as the activity's view

        mHandler = Handler(Looper.getMainLooper()) //initiates a Handler that uses the app's main looper (runs on the UI thread)
    }

    /**
     * Checking and Requesting Permissions
     */
    private fun checkAndRequestPermissions(){
        //For each permission in the REQUIRED_PERMISSION_LIST, if the device has not already granted this permission, add it to the missingPermission list
        for(eachPermission in REQUIRED_PERMISSION_LIST){
            if(ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED){
                missingPermission.add(eachPermission)
                Log.d(TAG, "missing permission: $eachPermission")
            }
        }
        if(missingPermission.isEmpty()){ //if there are no missing permissions, start SDK registration
            startSDKRegistration()
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){ //if there are missing permissions, request the user to grant the permissions
            showToast("Need to grant the permission")
            //Making the permission request. The result is handled in onRequestPermissionsResult()
            ActivityCompat.requestPermissions(this,
                missingPermission.toArray(arrayOfNulls<String>(missingPermission.size)),
                REQUEST_PERMISSION_CODE)
        }
    }

    /**
     * SDK Registration
     */
    private fun startSDKRegistration(){
        if(isRegistrationInProgress.compareAndSet(false, true)) { //if isRegistrationInProgress is false, set to true
            showToast("Registering, please wait...")

            //Getting an instance of the DJISDKManager and using it to register the app (requires API key in AndroidManifest)
            DJISDKManager.getInstance().registerApp(applicationContext, object: DJISDKManager.SDKManagerCallback {

                //checking registration
                override fun onRegister(error: DJIError?) {
                    error?.let{
                        //If registration is successful, start a connection to the DJI product.
                        if(error == DJISDKError.REGISTRATION_SUCCESS){
                            showToast("Register Success")
                            DJISDKManager.getInstance().startConnectionToProduct()
                        //If registration is unsuccessful, prompt user and log the registration error
                        }else{
                            showToast("Register sdk fails, please check the bundle id and network connection!")
                        }
                        Log.v(TAG, error.description)
                    }

                }

                //called when the remote controller disconnects from the user's mobile device
                override fun onProductDisconnect() {
                    Log.d(TAG, "onProductDisconnect")
                    showToast("Remote Controller Disconnected")
                    notifyStatusChange()
                }

                //called the remote controller connects to the user's mobile device
                override fun onProductConnect(product: BaseProduct?) {
                    Log.d(TAG, "onProductConnect newProduct: $product")
                    showToast("Remote Controller Connected")
                    notifyStatusChange()
                }

                //called when the DJI aircraft changes
                override fun onProductChanged(p0: BaseProduct?) {
                    if (p0.toString() == "None"){
                        showToast("aircraft disconnected")
                    }
                    else{
                        showToast("aircraft: $p0 connected")
                    }

                }
                
                //Called when a component object changes. This method is not called if the component is already disconnected
                override fun onComponentChange(
                    componentKey: BaseProduct.ComponentKey?,
                    oldComponent: BaseComponent?,
                    newComponent: BaseComponent?
                ) {
                    //Listen to connectivity changes in the new component
                    newComponent?.let{ it ->
                        it.setComponentListener {
                            Log.d(TAG, "onComponentConnectivityChanged: $it")
                            notifyStatusChange()
                        }
                    }

                    //Alert the user which component has changed, and mention what new component replaced the old component (can be null)
                    showToast("onComponentChange key: $componentKey, oldComponent: $oldComponent, newComponent: $newComponent")
                    Log.d(TAG, "onComponentChange key: $componentKey, oldComponent: $oldComponent, newComponent: $newComponent")

                }

                //called when loading SDK resources
                override fun onInitProcess(p0: DJISDKInitEvent?, p1: Int) {
                    //TODO
                }

                //Called when Fly Safe database download progress is updated
                override fun onDatabaseDownloadProgress(p0: Long, p1: Long) {
                    //TODO
                }

            })
        }

    }

    /**
     * Function used to notify the app of status changes
     */
    private fun notifyStatusChange(){
        mHandler.removeCallbacks(updateRunnable) //removes any pending posts of updateRunnable from the message queue
        mHandler.postDelayed(updateRunnable, 500) //adds a new updateRunnable to the message queue, which is executed 0.5 seconds after
    }

    /**
     * Runnable object (executable command) that sends a broadcast with a specific intent.
     */
    private val updateRunnable: Runnable = Runnable {
        val intent = Intent(FLAG_CONNECTION_CHANGE) //This intent lets the broadcast receiver (itself) know that a connection change has occurred.
        sendBroadcast(intent)
    }

    /**
     * Function displays a toast using provided string parameter
     */
    private fun showToast(text: String){
        val handler = Handler(Looper.getMainLooper())
        handler.post{
            Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Asynchronous function handles the results of ActivityCompat.requestPermissions()
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode,
            permissions,
            grantResults)
        //For every permission in the missingPermissions list, if the permission is granted, remove it from the list
        if(requestCode == REQUEST_PERMISSION_CODE){
            grantResults.size
            val index = grantResults.size-1
            for(i in index downTo 0){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    missingPermission.remove(permissions[i])
                }
            }
        }
        //if there are no missing permissions, start SDK registration
        if(missingPermission.isEmpty()){
            startSDKRegistration()
        }else{
            showToast("Missing Permissions!!")
        }
    }


}