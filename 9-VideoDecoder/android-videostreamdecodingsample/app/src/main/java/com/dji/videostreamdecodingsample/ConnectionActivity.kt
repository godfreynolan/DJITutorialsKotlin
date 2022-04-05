package com.dji.videostreamdecodingsample

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import dji.sdk.sdkmanager.DJISDKManager

/*
This activity manages SDK registration and establishing a connection between the
DJI product and the user's mobile phone.
 */
class ConnectionActivity : AppCompatActivity() {

    //Class Variables
    private lateinit var mTextConnectionStatus: TextView
    private lateinit var mTextProduct: TextView
    private lateinit var mTextModelAvailable: TextView
    private lateinit var mBtnOpen: Button
    private lateinit var mVersionTv: TextView

    private val model: ConnectionViewModel by viewModels() //linking the activity to a viewModel

    companion object {
        const val TAG = "ConnectionActivity"
    }

    //Creating the Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //inflating the activity_connection.xml layout as the activity's view
        setContentView(R.layout.activity_connection)

        /*
        Request the following permissions defined in the AndroidManifest.
        1 is the integer constant we chose to use when requesting app permissions
        */
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.READ_PHONE_STATE
            ), 1)

        //Initialize the UI, register the app with DJI's mobile SDK, and set up the observers
        initUI()
        model.registerApp()
        observers()
    }

    //Function to initialize the activity's UI
    private fun initUI() {

        //referencing the layout views using their resource ids
        mTextConnectionStatus = findViewById(R.id.text_connection_status)
        mTextModelAvailable = findViewById(R.id.text_model_available)
        mTextProduct = findViewById(R.id.text_product_info)
        mBtnOpen = findViewById(R.id.btn_open)
        mVersionTv = findViewById(R.id.textView2)

        //Getting the DJI SDK version and displaying it on mVersionTv TextView
        mVersionTv.text = resources.getString(R.string.sdk_version, DJISDKManager.getInstance().sdkVersion)

        mBtnOpen.isEnabled = false //mBtnOpen Button is initially disabled

        //If mBtnOpen Button is clicked on, start MainActivity (only works when button is enabled)
        mBtnOpen.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    //Function to setup observers
    private fun observers() {
        //observer listens to changes to the connectionStatus variable stored in the viewModel
        model.connectionStatus.observe(this, Observer<Boolean> { isConnected ->
            //If boolean is True, enable mBtnOpen button. If false, disable the button.
            if (isConnected) {
                mTextConnectionStatus.text = "Status: Connected"
                mBtnOpen.isEnabled = true
            }
            else {
                mTextConnectionStatus.text = "Status: Disconnected"
                mBtnOpen.isEnabled = false
            }
        })

        /*
        Observer listens to changes to the product variable stored in the viewModel.
        product is a BaseProduct object and represents the DJI product connected to the mobile device
        */
        model.product.observe(this, Observer { baseProduct ->
            //if baseProduct is connected to the mobile device, display its firmware version and model name.
            if (baseProduct != null && baseProduct.isConnected) {
                mTextModelAvailable.text = baseProduct.firmwarePackageVersion

                //name of the aircraft attached to the remote controller
                mTextProduct.text = baseProduct.model.displayName
            }
        })
    }
}