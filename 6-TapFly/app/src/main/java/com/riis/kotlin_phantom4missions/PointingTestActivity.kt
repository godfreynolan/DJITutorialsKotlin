package com.riis.kotlin_phantom4missions

import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import dji.common.mission.tapfly.TapFlyMission
import dji.common.mission.tapfly.TapFlyMissionState
import dji.common.mission.tapfly.TapFlyMode
import dji.sdk.mission.tapfly.TapFlyMissionOperator
import dji.sdk.sdkmanager.DJISDKManager

class PointingTestActivity : DemoBaseActivity(), TextureView.SurfaceTextureListener,
    View.OnClickListener, View.OnTouchListener {

    companion object {
        private val TAG = "PointingTestActivity"
    }


    private lateinit var mTapFlyMission: TapFlyMission
    private lateinit var mPushDrawerIb: ImageButton
    private lateinit var mPushDrawerSd: SlidingDrawer
    private lateinit var mStartBtn: Button
    private lateinit var mStopBtn: ImageButton
    private lateinit var mPushTv: TextView
    private lateinit var mBgLayout: RelativeLayout
    private lateinit var mRstPointIv: ImageView
    private lateinit var mAssisTv: TextView
    private lateinit var mAssisSw: Switch
    private lateinit var mSpeedTv: TextView
    private lateinit var mSpeedSb: SeekBar

    private fun getTapFlyOperator(): TapFlyMissionOperator {
        return DJISDKManager.getInstance().missionControl.tapFlyMissionOperator
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_pointing_test)
        super.onCreate(savedInstanceState)
        initUI()
        getTapFlyOperator().addListener { aggregation ->
            val executionState = aggregation?.executionState
            if (executionState != null) {
                showPointByTapFlyPoint(executionState.imageLocation, mRstPointIv)
            }
            val sb = StringBuffer()
            val errorInformation =
                (if (aggregation?.error == null) "null" else aggregation.let { it.error?.description + "\n" })
            val currentState =
                if (aggregation?.currentState == null) "null" else aggregation.currentState
                    .name
            val previousState =
                if (aggregation?.previousState == null) "null" else aggregation?.previousState?.name
            Utils.addLineToSB(sb, "CurrentState: ", currentState as Any)
            if (previousState != null) {
                Utils.addLineToSB(sb, "PreviousState: ", previousState)
            }
            Utils.addLineToSB(sb, "Error:", errorInformation)
            val progressState = aggregation?.executionState
            if (progressState != null) {
                Utils.addLineToSB(sb, "Heading: ", progressState.relativeHeading as Any)
                Utils.addLineToSB(sb, "PointX: ", progressState.imageLocation.x as Any)
                Utils.addLineToSB(sb, "PointY: ", progressState.imageLocation.y as Any)
                Utils.addLineToSB(
                    sb,
                    "BypassDirection: ",
                    progressState.bypassDirection.name
                )
                Utils.addLineToSB(sb, "VectorX: ", progressState.direction.x as Any)
                Utils.addLineToSB(sb, "VectorY: ", progressState.direction.y as Any)
                Utils.addLineToSB(sb, "VectorZ: ", progressState.direction.z as Any)
                setResultToText(sb.toString())
            }
            val missionState = aggregation?.currentState
            if (!(missionState == TapFlyMissionState.EXECUTING || missionState == TapFlyMissionState.EXECUTION_PAUSED
                        || missionState == TapFlyMissionState.EXECUTION_RESETTING)
            ) {
                setVisible(mRstPointIv, false)
                setVisible(mStopBtn, false)
            } else {
                setVisible(mStopBtn, true)
                setVisible(mStartBtn, false)
            }
        }

    }

    override fun onClick(v: View?) {
        v?.let { view ->
            if (view.id == R.id.pointing_drawer_control_ib) {
                if (mPushDrawerSd.isOpened) {
                    mPushDrawerSd.animateClose()
                } else {
                    mPushDrawerSd.animateOpen()
                }
                return
            }
            when (view.id) {
                R.id.pointing_start_btn -> getTapFlyOperator().startMission(mTapFlyMission) { error ->
                    setResultToToast(if (error == null) "Start Mission Successfully" else error.description)
                    if (error == null) {
                        setVisible(mStartBtn, false)
                    }
                }
                R.id.pointing_stop_btn -> getTapFlyOperator().stopMission { error ->
                    setResultToToast(
                        if (error == null) "Stop Mission Successfully" else error.description
                    )
                }
                else -> {
                    setResultToToast("TapFlyMission Operator is null")
                }
            }
        }

    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (v?.id == R.id.pointing_bg_layout) {
            event?.let { e ->
                when (e.action) {
                    MotionEvent.ACTION_UP -> {
                        mStartBtn.visibility = View.VISIBLE
                        mStartBtn.x = e.x - mStartBtn.width / 2
                        mStartBtn.y = e.y - mStartBtn.height / 2
                        mStartBtn.requestLayout()
                        mTapFlyMission.target = getTapFlyPoint(mStartBtn)
                    }
                    else -> {
                    }
                }
            }

        }
        return true
    }

    override fun onResume() {
        super.onResume()
        initTapFlyMission()
    }

    override fun onDestroy() {
        mCodecManager?.destroyCodec()
        super.onDestroy()
    }

    override fun onReturn(view: View) {
        Log.d(TAG, "onReturn")
        this.finish()
    }

    private fun setResultToToast(string: String) {
        runOnUiThread {
            Toast.makeText(this@PointingTestActivity, string, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setResultToText(string: String) {
        runOnUiThread { mPushTv.text = string }
    }

    private fun setVisible(v: View?, visible: Boolean) {
        if (v == null) return
        runOnUiThread { v.visibility = if (visible) View.VISIBLE else View.INVISIBLE }
    }

    private fun initUI() {
        mPushDrawerIb = findViewById<ImageButton>(R.id.pointing_drawer_control_ib)
        mPushDrawerSd = findViewById(R.id.pointing_drawer_sd)
        mStartBtn = findViewById(R.id.pointing_start_btn)
        mStopBtn = findViewById(R.id.pointing_stop_btn)
        mPushTv = findViewById(R.id.pointing_push_tv)
        mBgLayout = findViewById(R.id.pointing_bg_layout)
        mRstPointIv = findViewById(R.id.pointing_rst_point_iv)
        mAssisTv = findViewById(R.id.pointing_assistant_tv)
        mAssisSw = findViewById(R.id.pointing_assistant_sw)
        mSpeedTv = findViewById(R.id.pointing_speed_tv)
        mSpeedSb = findViewById(R.id.pointing_speed_sb)
        mPushDrawerIb.setOnClickListener(this)
        mStartBtn.setOnClickListener(this)
        mStopBtn.setOnClickListener(this)
        mBgLayout.setOnTouchListener(this)
        mSpeedSb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {

                mSpeedTv.text = (progress + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                getTapFlyOperator().setAutoFlightSpeed(
                    getSpeed()
                ) { error -> setResultToToast(if (error == null) "Set Auto Flight Speed Success" else error.description) }
            }
        })
    }

    private fun initTapFlyMission() {
        mTapFlyMission = TapFlyMission()
        mTapFlyMission.isHorizontalObstacleAvoidanceEnabled = mAssisSw.isChecked
        mTapFlyMission.tapFlyMode = TapFlyMode.FORWARD
    }

    private fun getTapFlyPoint(iv: View?): PointF? {
        if (iv == null) return null
        val parent = iv.parent as View
        var centerX = iv.left + iv.x + iv.width.toFloat() / 2
        var centerY = iv.top + iv.y + iv.height.toFloat() / 2
        centerX = if (centerX < 0) 0f else centerX
        centerX = if (centerX > parent.width) parent.width.toFloat() else centerX
        centerY = if (centerY < 0) 0f else centerY
        centerY = if (centerY > parent.height) parent.height.toFloat() else centerY
        return PointF(centerX / parent.width, centerY / parent.height)
    }

    private fun showPointByTapFlyPoint(
        point: PointF?,
        iv: ImageView?
    ) {
        if (point == null || iv == null) {
            return
        }
        val parent = iv.parent as View
        runOnUiThread {
            iv.x = point.x * parent.width - iv.width / 2
            iv.y = point.y * parent.height - iv.height / 2
            iv.visibility = View.VISIBLE
            iv.requestLayout()
        }
    }

    private fun getSpeed(): Float {
        return (mSpeedSb.progress + 1).toFloat()
    }


}