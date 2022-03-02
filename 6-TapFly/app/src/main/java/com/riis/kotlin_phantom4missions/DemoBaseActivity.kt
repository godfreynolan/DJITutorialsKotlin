package com.riis.kotlin_phantom4missions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import dji.common.product.Model
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.products.HandHeld
import dji.sdk.sdkmanager.DJISDKManager

open class DemoBaseActivity : FragmentActivity(), TextureView.SurfaceTextureListener {

    private val TAG = MainActivity::class.java.name
    protected var mReceivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    protected var mCodecManager: DJICodecManager? = null
    private var mProduct: BaseProduct? = null

    //To store index chosen in PopupNumberPicker listener
    protected var INDEX_CHOSEN = mutableListOf<Int>(-1, -1, -1)
    protected var mVideoSurface: TextureView? = null
    protected var mConnectionStatusTextView: TextView? = null

    protected val mReceiver = (object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTitleBar()
            onProductChange()
        }

    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter()
        filter.addAction("dji_sdk_connection_change")
        registerReceiver(mReceiver, filter)

        mVideoSurface = findViewById(R.id.video_previewer_surface)
        mConnectionStatusTextView = findViewById<TextView>(R.id.ConnectStatusTextView)

        if (mVideoSurface != null) {
            mVideoSurface?.surfaceTextureListener = this
        }

        mReceivedVideoDataListener = (VideoFeeder.VideoDataListener { videoBuffer, size ->
            if (mCodecManager != null) {
                mCodecManager?.sendDataToDecoder(videoBuffer, size)
            }
        })

    }

    protected open fun onProductChange() {
        initPreviewer()
    }

    private fun initPreviewer() {
        mProduct = try {
            getProductInstance()
        } catch (e: Exception) {
            null
        }

        if (mProduct == null) {
            mProduct?.isConnected?.let {
                if (!it) {
                    Log.d(TAG, "Disconnect")
                }
            }
        } else {
            if (mVideoSurface != null) {
                mVideoSurface?.surfaceTextureListener = this
            }

            if (mProduct != null) {
                (mProduct?.model?.equals(Model.UNKNOWN_AIRCRAFT))?.let {
                    if (!it) {
                        mReceivedVideoDataListener?.let { c ->
                            VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(
                                c
                            )
                        }
                    }
                }

            }

        }
    }

    private fun uninitPreviewer() {
        val camera: Camera? = getCameraInstance()
        if (camera != null) {
            VideoFeeder.getInstance().primaryVideoFeed.removeVideoDataListener(
                mReceivedVideoDataListener
            )
        }
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (mCodecManager == null) {
            mCodecManager = DJICodecManager(this, surface, width, height)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        if (mCodecManager != null) {
            mCodecManager?.cleanSurface()
        }
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

    }

    private fun updateTitleBar() {
        if (mConnectionStatusTextView == null) return
        var ret = false
        val product = getProductInstance()
        if (product != null) {
            if (product.isConnected) {
                mConnectionStatusTextView?.text =
                    "${getProductInstance()?.model?.displayName} Connected"
                ret = true
            } else {
                if (product is Aircraft) {
                    if (product.remoteController != null) {

                    }
                    if (product.remoteController != null && product.remoteController.isConnected) {
                        mConnectionStatusTextView?.text = "Only RC Connected"
                        ret = true
                    }
                }
            }
        }
        if (!ret) {
            mConnectionStatusTextView?.text = "Disconnected"
        }
    }

    open fun onReturn(view: View) {
        this.finish()
    }

    fun resetIndex() {
        INDEX_CHOSEN = mutableListOf(-1, -1, -1)
    }

    fun makeListHelper(o: Array<Any>): ArrayList<String> {
        val list = ArrayList<String>()
        for (i in 0 until o.size - 1) {
            list.add(o[i].toString())
        }
        return list
    }

    override fun onPause() {
        Log.e(TAG, "onPause")
        uninitPreviewer()
        super.onPause()
    }

    override fun onStop() {
        Log.e(TAG, "onStop")
        super.onStop()
    }

    override fun onResume() {
        Log.e(TAG, "onResume")
        super.onResume()
        updateTitleBar()
        initPreviewer()
        onProductChange()

        if (mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null")
        }
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        unregisterReceiver(mReceiver)
        uninitPreviewer()
        super.onDestroy()

    }

    private fun getProductInstance(): BaseProduct? {
        return DJISDKManager.getInstance().product
    }

    private fun getCameraInstance(): Camera? {
        if (getProductInstance() == null) return null
        if (getProductInstance() is Aircraft) {
            return (getProductInstance() as Aircraft).camera
        } else if (getProductInstance() is HandHeld) {
            return (getProductInstance() as HandHeld).camera
        } else
            return null
    }

    private fun isProductModuleAvailable(): Boolean {
        return (getProductInstance() != null)
    }

    private fun isCameraModuleAvailable(): Boolean {
        return isProductModuleAvailable() && (getProductInstance()?.camera != null)
    }


}