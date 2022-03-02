package com.riis.kotlin_simulatordemo

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import dji.common.flightcontroller.simulator.InitializationData
import dji.common.flightcontroller.virtualstick.*
import dji.common.model.LocationCoordinate2D
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var mConnectStatusTextView: TextView
    private lateinit var mBtnEnableVirtualStick: Button
    private lateinit var mBtnDisableVirtualStick: Button
    private lateinit var mBtnSimulator: ToggleButton
    private lateinit var mBtnTakeOff: Button
    private lateinit var mBtnLand: Button
    private lateinit var mTextView: TextView
    private lateinit var mScreenJoystickLeft: OnScreenJoystick
    private lateinit var mScreenJoystickRight: OnScreenJoystick

    private var mSendVirtualStickDataTimer: Timer? = null
    private var mSendVirtualStickDataTask: SendVirtualStickDataTask? = null

    private var mPitch: Float = 0f
    private var mRoll: Float = 0f
    private var mYaw: Float = 0f
    private var mThrottle: Float = 0f

    private val viewModel by viewModels<MainViewModel>()

    companion object {
        const val TAG = "UserAppMainAct"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this,
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
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO
            ), 1
        )
        viewModel.startSdkRegistration(this)
        initObservers()
        initUi()

    }

    private fun initObservers() {
        viewModel.connectionStatus.observe(this, androidx.lifecycle.Observer<Boolean> {
            initFlightController()
            var ret = false
            viewModel.product?.let {
                if (it.isConnected) {
                    mConnectStatusTextView.text = it.model.toString() + " Connected"
                    ret = true
                } else {
                    if ((it as Aircraft?)?.remoteController != null && it.remoteController.isConnected
                    ) {
                        mConnectStatusTextView.text = "only RC Connected"
                        ret = true
                    }

                }
            }
            if (!ret) {
                mConnectStatusTextView.text = "Disconnected"
            }
        })
    }

    private fun initUi() {
        mBtnEnableVirtualStick = findViewById(R.id.btn_enable_virtual_stick)
        mBtnDisableVirtualStick = findViewById(R.id.btn_disable_virtual_stick)
        mBtnTakeOff = findViewById(R.id.btn_take_off)
        mBtnLand = findViewById(R.id.btn_land)
        mBtnSimulator = findViewById(R.id.btn_start_simulator)
        mTextView = findViewById(R.id.textview_simulator)
        mConnectStatusTextView = findViewById(R.id.ConnectStatusTextView)
        mScreenJoystickRight = findViewById(R.id.directionJoystickRight)
        mScreenJoystickLeft = findViewById(R.id.directionJoystickLeft)

        mBtnEnableVirtualStick.setOnClickListener(this)
        mBtnDisableVirtualStick.setOnClickListener(this)
        mBtnTakeOff.setOnClickListener(this)
        mBtnLand.setOnClickListener(this)

        mBtnSimulator.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                mTextView.visibility = View.VISIBLE
                viewModel.getFlightController()?.simulator?.start(
                    InitializationData.createInstance(
                        LocationCoordinate2D(23.0, 113.0), 10, 10
                    )
                ) { djiError ->
                    if (djiError != null) {
                        Log.i(TAG, djiError.description)
                        showToast("Simulator Error: ${djiError.description}")
                    } else {
                        Log.i(TAG,"Start Simulator Success")
                        showToast("Start Simulator Success")
                    }
                }
            } else {
                mTextView.visibility = View.INVISIBLE
                if(viewModel.getFlightController() == null) {
                    Log.i(TAG, "isFlightController Null ")
                }
                viewModel.getFlightController()?.simulator?.stop { djiError ->
                    if (djiError != null) {
                        Log.i(TAG, djiError.description)
                        showToast("Simulator Error: ${djiError.description}")
                    } else {
                        Log.i(TAG,"Stop Simulator Success")
                        showToast("Stop Simulator Success")
                    }
                }
            }
        }

        mScreenJoystickRight.setJoystickListener(object : OnScreenJoystickListener {
            override fun onTouch(joystick: OnScreenJoystick?, pXP: Float, pYP: Float) {
                var pX = pXP
                var pY = pYP
                if (abs(pX) < 0.02) {
                    pX = 0f
                }
                if (abs(pY) < 0.02) {
                    pY = 0f
                }
                val pitchJoyControlMaxSpeed = 10f
                val rollJoyControlMaxSpeed = 10f
                mPitch = (pitchJoyControlMaxSpeed * pX)
                mRoll = (rollJoyControlMaxSpeed * pY)
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
                    mSendVirtualStickDataTimer = Timer()
                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 100, 200)
                }
            }
        })

        mScreenJoystickLeft.setJoystickListener(object : OnScreenJoystickListener {
            override fun onTouch(joystick: OnScreenJoystick?, pX: Float, pY: Float) {
                var pX = pX
                var pY = pY
                if (abs(pX) < 0.02) {
                    pX = 0f
                }
                if (abs(pY) < 0.02) {
                    pY = 0f
                }
                val verticalJoyControlMaxSpeed = 2f
                val yawJoyControlMaxSpeed = 30f
                mYaw = (yawJoyControlMaxSpeed * pX)
                mThrottle = (verticalJoyControlMaxSpeed * pY)
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
                    mSendVirtualStickDataTimer = Timer()
                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 0, 200)
                }
            }
        })
    }

    private fun initFlightController() {

        viewModel.getFlightController()?.let {
            it.rollPitchControlMode = RollPitchControlMode.VELOCITY
            it.yawControlMode = YawControlMode.ANGULAR_VELOCITY
            it.verticalControlMode = VerticalControlMode.VELOCITY
            it.rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
            it.simulator.setStateCallback { stateData ->
                val yaw = String.format("%.2f", stateData.yaw)
                val pitch = String.format("%.2f", stateData.pitch)
                val roll = String.format("%.2f", stateData.roll)
                val positionX = String.format("%.2f", stateData.positionX)
                val positionY = String.format("%.2f", stateData.positionY)
                val positionZ = String.format("%.2f", stateData.positionZ)

                lifecycleScope.launch(Dispatchers.Main) {
                    mTextView.text = "Yaw: $yaw, Pitch $pitch, Roll: $roll, \n, PosX: $positionX, PosY: $positionY, PosZ: $positionZ"
                }

            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_enable_virtual_stick -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.setVirtualStickModeEnabled(true) { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Virtual Stick: Could not enable virtual stick")
                        } else {
                            Log.i(TAG,"Enable Virtual Stick Success")
                            showToast("Virtual Sticks Enabled")
                        }
                    }
                }

            }
            R.id.btn_disable_virtual_stick -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.setVirtualStickModeEnabled(false) { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Virtual Stick: Could not disable virtual stick")
                        } else {
                            Log.i(TAG,"Disable Virtual Stick Success")
                            showToast("Virtual Sticks Disabled")
                        }
                    }
                }
            }
            R.id.btn_take_off -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.startTakeoff { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Takeoff Error: ${djiError.description}")
                        } else {
                            Log.i(TAG,"Takeoff Success")
                            showToast("Takeoff Success")
                        }
                    }
                }
            }
            R.id.btn_land -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.startLanding { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Landing Error: ${djiError.description}")
                        } else {
                            Log.i(TAG,"Start Landing Success")
                            showToast("Start Landing Success")
                        }
                    }
                }
            }
            else -> {

            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun onReturn(view: View) {
        this.finish()
    }

    override fun onDestroy() {
        mSendVirtualStickDataTask?.let {
            it.cancel()
            mSendVirtualStickDataTask = null
        }

        mSendVirtualStickDataTimer?.let {
            it.cancel()
            it.purge()
            mSendVirtualStickDataTimer = null
        }
        super.onDestroy()
    }

    inner class SendVirtualStickDataTask: TimerTask() {
        override fun run() {
            viewModel.getFlightController()?.sendVirtualStickFlightControlData(
                FlightControlData(mPitch, mRoll, mYaw, mThrottle)
            ) {

            }
        }
    }
}