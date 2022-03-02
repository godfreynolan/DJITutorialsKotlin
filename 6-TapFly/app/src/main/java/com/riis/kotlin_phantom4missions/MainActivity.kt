package com.riis.kotlin_phantom4missions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.useraccount.UserAccountManager
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : DemoBaseActivity() {


    companion object {
        val TAG = MainActivity::class.java.name

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

        private class DemoInfo(
            val title: String,
            val desc: String,
            val demoClass: Class<out android.app.Activity>
        ) {

        }

        private val REQUEST_PERMISSION_CODE = 125
    }

    var mString: String? = null
    var mProduct: BaseProduct? = null
    private val demos = ArrayList<DemoInfo>()
    private var mListView: ListView? = null
    private var mDemoListAdapter = DemoListAdapter()
    private var mFirmwareVersionView: TextView? = null
    var mConnectStatusView: TextView? = null


    private val missingPermission = ArrayList<String>()
    private val isRegistrationInProgress = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContentView(R.layout.activity_main)

        mConnectStatusView = findViewById<TextView>(R.id.ConnectStatusTextView)
        mListView = findViewById(R.id.listView)
        mListView?.adapter = mDemoListAdapter
        mFirmwareVersionView = findViewById(R.id.version_tv)
        loadDemoList()
        mDemoListAdapter.notifyDataSetChanged()
        //updateVersion()
    }


    private fun checkAndRequestPermissions() {
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    eachPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermission.add(eachPermission)
            }
        }
        if (missingPermission.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                missingPermission.toArray(arrayOfNulls<String>(missingPermission.size)),
                REQUEST_PERMISSION_CODE
            )
        } else if (missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startSDKRegistration()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.size - 1 downTo 0) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                }
            }
        }
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else {
            showToast("Missing permissions!!!")
        }
    }


    private fun startSDKRegistration() {
        Log.i(TAG, ("outside register"))
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            Log.i(TAG, ("registering, pls wait..."))
            DJISDKManager.getInstance()
                .registerApp(applicationContext, object : DJISDKManager.SDKManagerCallback {
                    override fun onRegister(djiError: DJIError?) {
                        Log.e("Register", "Inside")
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            Log.e("App registration", DJISDKError.REGISTRATION_SUCCESS.description)
                            DJISDKManager.getInstance().startConnectionToProduct()
                            Log.e("Register", "Success")
                        } else {
                            Log.e("Register", "Failed")
                            showToast("Register sdk fails, check network is available")
                        }
                        Log.v(TAG, "${djiError?.description}")
                    }

                    override fun onProductDisconnect() {
                        Log.d(TAG, "onProductDisconnect")
                        showToast("Product Disconnected")
                    }

                    override fun onProductConnect(baseProduct: BaseProduct?) {
                        Log.d(TAG, "onProductConnect newProduct: $baseProduct")
                        showToast("Product Connected")
                    }

                    override fun onComponentChange(
                        componentKey: BaseProduct.ComponentKey?,
                        oldComponent: BaseComponent?,
                        newComponent: BaseComponent?
                    ) {
                        newComponent?.setComponentListener { isConnected ->
                            Log.d(TAG, "onComponentConnectivityChanged: $isConnected")
                        }
                        Log.d(
                            TAG,
                            "onComponentChange key: $componentKey, oldComponent: $oldComponent, newComponent: $newComponent"
                        )
                    }

                    override fun onInitProcess(p0: DJISDKInitEvent?, p1: Int) {

                    }

                    override fun onProductChanged(p0: BaseProduct?) {

                    }

                    override fun onDatabaseDownloadProgress(p0: Long, p1: Long) {

                    }

                })

        }
    }

    private fun loadDemoList() {
        mListView?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                onListItemClick(position)
            }
        demos.clear()
        demos.add(
            DemoInfo(
                getString(R.string.title_activity_tracking_test),
                getString(R.string.demo_desc_tracking),
                TrackingTestActivity::class.java
            )
        )
        demos.add(
            DemoInfo(
                getString(R.string.title_activity_pointing_test),
                getString(R.string.demo_desc_pointing),
                PointingTestActivity::class.java
            )
        )
    }

    private fun onListItemClick(position: Int) {
        var intent: Intent? = null
        intent = Intent(this, demos[position].demoClass)
        Log.i("Track", "Item Clicked")
        this.startActivity(intent)

    }

    private fun showToast(text: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
        }
    }

    private inner class DemoListAdapter : BaseAdapter() {

        override fun getCount(): Int {
            return demos.size
        }

        override fun getItem(position: Int): Any {
            return demos[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = View.inflate(this@MainActivity, R.layout.demo_info_item, null)
            if (v != null) {
                Log.e(
                    TAG,
                    "Convert View not null: ${demos[position].title.toString()} ${demos[position].desc.toString()}"
                )
                val title: TextView = v.findViewById(R.id.title)
                val desc: TextView = v.findViewById(R.id.desc)
                title.text = demos[position].title
                desc.text = demos[position].desc
            }

            return v
        }

    }

    private fun setResultToToast(string: String) {
        runOnUiThread { Toast.makeText(this@MainActivity, string, Toast.LENGTH_SHORT).show() }
    }

    private fun loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(
            this,
            object : CommonCallbacks.CompletionCallbackWith<UserAccountState?> {
                override fun onSuccess(userAccountState: UserAccountState?) {
                    Log.d(TAG, "Login Success")
                }

                override fun onFailure(djiError: DJIError) {
                    setResultToToast("Login Failed: " + djiError.description)
                }
            })
    }

    var version: String? = null

    private fun updateVersion() {
        val product = DJISDKManager.getInstance().product
        if (product != null) {
            version = product.firmwarePackageVersion
        }
        if (version == null) {
            version = "N/A"
        }
        runOnUiThread { mFirmwareVersionView!!.text = "Firmware version: $version" }
    }

    override fun onProductChange() {
        super.onProductChange()
        loadDemoList()
        mDemoListAdapter.notifyDataSetChanged()
        updateVersion()
        //loginAccount()
    }


}