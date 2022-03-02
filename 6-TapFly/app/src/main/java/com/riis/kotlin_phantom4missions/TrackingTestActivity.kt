package com.riis.kotlin_phantom4missions

import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.*
import dji.common.camera.SettingsDefinitions.StorageLocation
import dji.common.error.DJIError
import dji.common.mission.activetrack.*
import dji.common.util.CommonCallbacks
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.keysdk.FlightControllerKey
import dji.keysdk.KeyManager
import dji.keysdk.callback.ActionCallback
import dji.keysdk.callback.SetCallback
import dji.midware.media.DJIVideoDataRecver
import dji.sdk.mission.MissionControl
import dji.sdk.mission.activetrack.ActiveTrackMissionOperatorListener
import dji.sdk.mission.activetrack.ActiveTrackOperator
import dji.sdk.sdkmanager.DJISDKManager
import java.util.concurrent.ConcurrentHashMap

class TrackingTestActivity : DemoBaseActivity(), TextureView.SurfaceTextureListener,
    View.OnClickListener, View.OnTouchListener, CompoundButton.OnCheckedChangeListener,
    ActiveTrackMissionOperatorListener {

    companion object {
        private val TAG = "TrackingtestActivity"
        private val MAIN_CAMERA_INDEX = 0
        private val INVALID_INDEX = -1
        private val MOVE_OFFSET = 20
    }

    private lateinit var layoutParams: RelativeLayout.LayoutParams
    private lateinit var mAutoSensingSw: Switch
    private lateinit var mQuickShotSw: Switch
    private lateinit var mPushDrawerIb: ImageButton
    private lateinit var mPushInfoSd: SlidingDrawer
    private lateinit var mStopBtn: ImageButton
    private lateinit var mTrackingImage: ImageView
    private lateinit var mBgLayout: RelativeLayout
    private lateinit var mPushInfoTv: TextView
    private lateinit var mPushBackSw: Switch
    private lateinit var mGestureModeSw: Switch
    private lateinit var mSendRectIV: ImageView
    private lateinit var mConfigBtn: Button
    private lateinit var mConfirmBtn: Button
    private lateinit var mRejectBtn: Button

    private var mActiveTrackOperator: ActiveTrackOperator? = null
    private lateinit var mActiveTrackMission: ActiveTrackMission
    private val trackModeKey: DJIKey? =
        FlightControllerKey.createFlightAssistantKey(FlightControllerKey.ACTIVE_TRACK_MODE)
    private val targetViewHashMap: ConcurrentHashMap<Int, MultiTrackingView> =
        ConcurrentHashMap<Int, MultiTrackingView>()
    private var trackingIndex: Int = TrackingTestActivity.INVALID_INDEX
    private var isAutoSensingSupported = false
    private var startMode = ActiveTrackMode.TRACE
    private var quickShotMode = QuickShotMode.UNKNOWN

    private var isDrawingRect = false


    private fun setResultToToast(string: String) {
        runOnUiThread {
            Toast.makeText(this@TrackingTestActivity, string, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Push Status to TextView
     *
     * @param string
     */
    private fun setResultToText(string: String) {
        runOnUiThread { mPushInfoTv.text = string }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_tracking_test)
        super.onCreate(savedInstanceState)
        initUI()
        initMissionManager()
    }

    /**
     * InitUI
     */
    private fun initUI() {
        layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        mPushDrawerIb = findViewById(R.id.tracking_drawer_control_ib)
        mPushInfoSd = findViewById(R.id.tracking_drawer_sd)
        mPushInfoTv = findViewById(R.id.tracking_push_tv)
        mBgLayout = findViewById(R.id.tracking_bg_layout)
        mSendRectIV = findViewById(R.id.tracking_send_rect_iv)
        mTrackingImage = findViewById(R.id.tracking_rst_rect_iv)
        mConfirmBtn = findViewById(R.id.confirm_btn)
        mStopBtn = findViewById(R.id.tracking_stop_btn)
        mRejectBtn = findViewById(R.id.reject_btn)
        mConfigBtn = findViewById(R.id.recommended_configuration_btn)
        mAutoSensingSw = findViewById(R.id.set_multitracking_enabled)
        mQuickShotSw = findViewById(R.id.set_multiquickshot_enabled)
        mPushBackSw = findViewById(R.id.tracking_pull_back_tb)
        mGestureModeSw = findViewById(R.id.tracking_in_gesture_mode)
        Log.d(TAG, "Initializing components...")
        mAutoSensingSw.isChecked = false
        mGestureModeSw.isChecked = false
        mQuickShotSw.isChecked = false
        mPushBackSw.isChecked = false
        mAutoSensingSw.setOnCheckedChangeListener(this)
        mQuickShotSw.setOnCheckedChangeListener(this)
        mPushBackSw.setOnCheckedChangeListener(this)
        mGestureModeSw.setOnCheckedChangeListener(this)
        mBgLayout.setOnTouchListener(this)
        mConfirmBtn.setOnClickListener(this)
        mStopBtn.setOnClickListener(this)
        mRejectBtn.setOnClickListener(this)
        mConfigBtn.setOnClickListener(this)
        mPushDrawerIb.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()

        initMissionManager()
    }

    /**
     * Init Mission parameter
     */
    private fun initMissionManager() {
        mActiveTrackOperator = MissionControl.getInstance().activeTrackOperator
        if (mActiveTrackOperator == null) {
            return
        }
        mActiveTrackOperator?.let { trackOperator ->
            trackOperator.addListener(this)
            mAutoSensingSw.isChecked = trackOperator.isAutoSensingEnabled
            mQuickShotSw.isChecked = trackOperator.isAutoSensingForQuickShotEnabled
            mGestureModeSw.isChecked = trackOperator.isGestureModeEnabled
            trackOperator.getRetreatEnabled(object :
                CommonCallbacks.CompletionCallbackWith<Boolean?> {
                override fun onSuccess(aBoolean: Boolean?) {
                    if (aBoolean != null) {
                        Log.e(TAG, "Retreat Not null")
                        mPushBackSw.isChecked = aBoolean
                        Log.e(TAG, "Retreat Enabled: $aBoolean")
                    }
                }

                override fun onFailure(error: DJIError) {
                    setResultToToast("can't get retreat enable state " + error.description)
                }
            })
        }
    }


    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     */
    override fun onReturn(view: View) {
        //DJILog.d(TAG, "onReturn")
        DJISDKManager.getInstance().missionControl.destroy()
        finish()
    }

    override fun onDestroy() {
        isAutoSensingSupported = false
        try {
            DJIVideoDataRecver.getInstance().setVideoDataListener(false, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (mActiveTrackOperator != null) {
            mActiveTrackOperator?.removeListener(this)
        }
        if (mCodecManager != null) {
            mCodecManager?.destroyCodec()
        }
        super.onDestroy()
    }

    var downX = 0f
    var downY = 0f

    /**
     * Calculate distance
     *
     * @param point1X
     * @param point1Y
     * @param point2X
     * @param point2Y
     * @return
     */
    private fun calcManhattanDistance(
        point1X: Double, point1Y: Double, point2X: Double,
        point2Y: Double
    ): Double {
        return Math.abs(point1X - point2X) + Math.abs(point1Y - point2Y)
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDrawingRect = false
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (calcManhattanDistance(
                        downX.toDouble(),
                        downY.toDouble(),
                        event.x.toDouble(),
                        event.y.toDouble()
                    ) < MOVE_OFFSET && !isDrawingRect
                ) {
                    trackingIndex = getTrackingIndex(downX, downY, targetViewHashMap)
                    if (targetViewHashMap[trackingIndex] != null) {
                        targetViewHashMap[trackingIndex]?.setBackgroundColor(Color.RED)
                    }
                    return true
                }
                isDrawingRect = true
                mSendRectIV.visibility = View.VISIBLE
                val l = (if (downX < event.x) downX else event.x).toInt()
                val t = (if (downY < event.y) downY else event.y).toInt()
                val r = (if (downX >= event.x) downX else event.x).toInt()
                val b = (if (downY >= event.y) downY else event.y).toInt()
                mSendRectIV.x = l.toFloat()
                mSendRectIV.y = t.toFloat()
                mSendRectIV.layoutParams.width = r - l
                mSendRectIV.layoutParams.height = b - t
                mSendRectIV.requestLayout()
            }
            MotionEvent.ACTION_UP -> if (mGestureModeSw.isChecked) {
                setResultToToast("Please try to start Gesture Mode!")
            } else if (!isDrawingRect) {
                targetViewHashMap?.let {
                    setResultToToast("Selected Index: $trackingIndex,Please Confirm it!")
                    targetViewHashMap[trackingIndex]?.setBackgroundColor(Color.TRANSPARENT)
                }

            } else {
                val rectF = getActiveTrackRect(mSendRectIV)
                mActiveTrackMission = ActiveTrackMission(rectF, startMode)
                if (startMode == ActiveTrackMode.QUICK_SHOT) {
                    mActiveTrackMission?.quickShotMode = quickShotMode
                    checkStorageStates()
                }
                mActiveTrackMission?.let {
                    mActiveTrackOperator?.startTracking(it) { error ->
                        if (error == null) {
                            Log.e("Main-Track", "Error = null")
                            isDrawingRect = false
                        }
                        setResultToToast("Start Tracking: " + if (error == null) "Success" else error.description)
                    }
                }

                mSendRectIV.visibility = View.INVISIBLE
                clearCurrentView()
            }
            else -> {
            }
        }
        return true
    }

    /**
     * @return
     */
    private fun getTrackingIndex(
        x: Float, y: Float,
        multiTrackinghMap: ConcurrentHashMap<Int, MultiTrackingView>?
    ): Int {
        if (multiTrackinghMap == null || multiTrackinghMap.isEmpty()) {
            return TrackingTestActivity.INVALID_INDEX
        }
        var l: Float
        var t: Float
        var r: Float
        var b: Float
        for ((key, view) in multiTrackinghMap) {
            l = view.x
            t = view.y
            r = view.x + view.width / 2
            b = view.y + view.height / 2
            if (x >= l && y >= t && x <= r && y <= b) {
                return key
            }
        }
        return TrackingTestActivity.INVALID_INDEX
    }

    override fun onClick(v: View) {
        if (mActiveTrackOperator == null) {
            Log.e(TAG, "mActive null")
            return
        }
        when (v.id) {
            R.id.recommended_configuration_btn -> {
                Log.e(TAG, "Config pressed")
                mActiveTrackOperator?.setRecommendedConfiguration(CommonCallbacks.CompletionCallback { error: DJIError ->
                    setResultToToast(
                        "Set Recommended Config " + error.description
                    )
                })
                runOnUiThread { mConfigBtn.visibility = View.GONE }
            }
            R.id.confirm_btn -> {
                mActiveTrackOperator?.let {
                    val isAutoTracking = isAutoSensingSupported &&
                            (it.isAutoSensingEnabled ||
                                    it.isAutoSensingForQuickShotEnabled)
                    if (isAutoTracking) {
                        startAutoSensingMission()
                        runOnUiThread {
                            mStopBtn.visibility = View.VISIBLE
                            mRejectBtn.visibility = View.VISIBLE
                            mConfirmBtn.visibility = View.INVISIBLE
                        }
                    } else {
                        trackingIndex = TrackingTestActivity.INVALID_INDEX
                        it.acceptConfirmation(CommonCallbacks.CompletionCallback { error: DJIError ->
                            setResultToToast(
                                error.description
                            )
                        })
                        runOnUiThread {
                            mStopBtn.visibility = View.VISIBLE
                            mRejectBtn.visibility = View.VISIBLE
                            mConfirmBtn.visibility = View.INVISIBLE
                        }
                    }
                }

            }
            R.id.tracking_stop_btn -> {
                trackingIndex = TrackingTestActivity.INVALID_INDEX

                mActiveTrackOperator?.stopTracking(CommonCallbacks.CompletionCallback { error: DJIError ->
                    setResultToToast(
                        error.description
                    )
                })


                runOnUiThread {
                    mTrackingImage.visibility = View.INVISIBLE
                    mSendRectIV.visibility = View.INVISIBLE
                    mStopBtn.visibility = View.INVISIBLE
                    mRejectBtn.visibility = View.INVISIBLE
                    mConfirmBtn.visibility = View.VISIBLE
                }
            }
            R.id.reject_btn -> {
                trackingIndex = TrackingTestActivity.INVALID_INDEX
                mActiveTrackOperator?.rejectConfirmation(CommonCallbacks.CompletionCallback { error: DJIError ->
                    setResultToToast(
                        error.description
                    )
                })
                runOnUiThread {
                    mStopBtn.visibility = View.VISIBLE
                    mRejectBtn.visibility = View.VISIBLE
                    mConfirmBtn.visibility = View.INVISIBLE
                }
            }
            R.id.tracking_drawer_control_ib -> if (mPushInfoSd.isOpened) {
                mPushInfoSd.animateClose()
            } else {
                mPushInfoSd.animateOpen()
            }
            else -> {
            }
        }
    }

    override fun onCheckedChanged(
        compoundButton: CompoundButton,
        isChecked: Boolean
    ) {
        if (mActiveTrackOperator == null) {
            return
        }
        Log.e(TAG, "onCheckChanged after null check")
        when (compoundButton.id) {
            R.id.set_multitracking_enabled -> {
                startMode = ActiveTrackMode.TRACE
                quickShotMode = QuickShotMode.UNKNOWN
                setAutoSensingEnabled(isChecked)
            }
            R.id.set_multiquickshot_enabled -> {
                startMode = ActiveTrackMode.QUICK_SHOT
                quickShotMode = QuickShotMode.CIRCLE
                checkStorageStates()
                setAutoSensingForQuickShotEnabled(isChecked)
            }
            R.id.tracking_pull_back_tb -> mActiveTrackOperator?.let {
                it.setRetreatEnabled(
                    isChecked,
                    CommonCallbacks.CompletionCallback { error: DJIError? ->
                        if (error != null) {
                            runOnUiThread { mPushBackSw.isChecked = !isChecked }
                        }
                        setResultToToast("Set Retreat Enabled: " + if (error == null) "Success" else error.description)
                    })

            }
            R.id.tracking_in_gesture_mode -> mActiveTrackOperator?.let {
                it.setGestureModeEnabled(
                    isChecked,
                    CommonCallbacks.CompletionCallback { error: DJIError ->
                        runOnUiThread { mGestureModeSw.isChecked = !isChecked }
                        setResultToToast("Set GestureMode Enabled: " + error.description)
                    })
            }
            else -> {
            }
        }
        Log.e(TAG, "onCheckChanged after when")
    }

    override fun onUpdate(event: ActiveTrackMissionEvent) {
        val sb = StringBuffer()
        val errorInformation =
            """
            ${if (event.error == null) "null" else event.error?.description}
            
            """.trimIndent()
        val currentState =
            if (event.currentState == null) "null" else event.currentState?.name
        val previousState =
            if (event.previousState == null) "null" else event.previousState?.name
        var targetState: ActiveTrackTargetState? = ActiveTrackTargetState.UNKNOWN
        if (event.trackingState != null) {
            targetState = event.trackingState?.state
        }
        Utils.addLineToSB(sb, "CurrentState: ", currentState as Any)
        Utils.addLineToSB(sb, "PreviousState: ", previousState as Any)
        Utils.addLineToSB(sb, "TargetState: ", targetState as Any)
        Utils.addLineToSB(sb, "Error:", errorInformation)


        val value = trackModeKey?.let { KeyManager.getInstance().getValue(it) }

        if (value != null) {
            if (value is ActiveTrackMode) {
                Utils.addLineToSB(sb, "TrackingMode:", value.toString())
            }
        }
        val trackingState = event.trackingState
        if (trackingState != null) {
            val targetSensingInformations =
                trackingState.autoSensedSubjects
            if (targetSensingInformations != null) {
                for (subjectSensingState in targetSensingInformations) {
                    val trackingRect = subjectSensingState.targetRect
                    if (trackingRect != null) {
                        Utils.addLineToSB(sb, "Rect center x: ", trackingRect.centerX())
                        Utils.addLineToSB(sb, "Rect center y: ", trackingRect.centerY())
                        Utils.addLineToSB(sb, "Rect Width: ", trackingRect.width())
                        Utils.addLineToSB(sb, "Rect Height: ", trackingRect.height())
                        Utils.addLineToSB(sb, "Reason", trackingState.reason.name)
                        Utils.addLineToSB(sb, "Target Index: ", subjectSensingState.index)
                        Utils.addLineToSB(
                            sb,
                            "Target Type",
                            subjectSensingState.targetType.name
                        )
                        Utils.addLineToSB(sb, "Target State", subjectSensingState.state.name)
                        isAutoSensingSupported = true
                    }
                }
            } else {
                val trackingRect = trackingState.targetRect
                if (trackingRect != null) {
                    Utils.addLineToSB(sb, "Rect center x: ", trackingRect.centerX())
                    Utils.addLineToSB(sb, "Rect center y: ", trackingRect.centerY())
                    Utils.addLineToSB(sb, "Rect Width: ", trackingRect.width())
                    Utils.addLineToSB(sb, "Rect Height: ", trackingRect.height())
                    Utils.addLineToSB(sb, "Reason", trackingState.reason.name)
                    Utils.addLineToSB(sb, "Target Index: ", trackingState.targetIndex)
                    Utils.addLineToSB(sb, "Target Type", trackingState.type?.name as Any)
                    Utils.addLineToSB(sb, "Target State", trackingState.state?.name as Any)
                    isAutoSensingSupported = false
                }
                clearCurrentView()
            }
        }
        setResultToText(sb.toString())
        updateActiveTrackRect(mTrackingImage, event)
        updateButtonVisibility(event)
    }

    /**
     * Update ActiveTrack Rect
     *
     * @param iv
     * @param event
     */
    private fun updateActiveTrackRect(
        iv: ImageView?,
        event: ActiveTrackMissionEvent?
    ) {
        if (iv == null || event == null) {
            return
        }
        val trackingState = event.trackingState
        if (trackingState != null) {
            if (trackingState.autoSensedSubjects != null) {
                val targetSensingInformations =
                    trackingState.autoSensedSubjects
                runOnUiThread {
                    if (targetSensingInformations != null) {
                        updateMultiTrackingView(targetSensingInformations)
                    }
                }
            } else {
                val trackingRect = trackingState.targetRect
                val trackTargetState = trackingState.state
                if (trackingRect != null) {
                    if (trackTargetState != null) {
                        postResultRect(iv, trackingRect, trackTargetState)
                    }
                }
            }
        }
    }

    private fun updateButtonVisibility(event: ActiveTrackMissionEvent) {
        val state = event.currentState
        if (state == ActiveTrackState.AUTO_SENSING || state == ActiveTrackState.AUTO_SENSING_FOR_QUICK_SHOT || state == ActiveTrackState.WAITING_FOR_CONFIRMATION
        ) {
            runOnUiThread {
                mStopBtn.visibility = View.VISIBLE
                mStopBtn.isClickable = true
                mConfirmBtn.visibility = View.VISIBLE
                mConfirmBtn.isClickable = true
                mRejectBtn.visibility = View.VISIBLE
                mRejectBtn.isClickable = true
                mConfigBtn.visibility = View.GONE
            }
        } else if (state == ActiveTrackState.AIRCRAFT_FOLLOWING || state == ActiveTrackState.ONLY_CAMERA_FOLLOWING || state == ActiveTrackState.FINDING_TRACKED_TARGET || state == ActiveTrackState.CANNOT_CONFIRM || state == ActiveTrackState.PERFORMING_QUICK_SHOT
        ) {
            runOnUiThread {
                mStopBtn.visibility = View.VISIBLE
                mStopBtn.isClickable = true
                mConfirmBtn.visibility = View.INVISIBLE
                mConfirmBtn.isClickable = false
                mRejectBtn.visibility = View.VISIBLE
                mRejectBtn.isClickable = true
                mConfigBtn.visibility = View.GONE
            }
        } else {
            runOnUiThread {
                mStopBtn.visibility = View.INVISIBLE
                mStopBtn.isClickable = false
                mConfirmBtn.visibility = View.INVISIBLE
                mConfirmBtn.isClickable = false
                mRejectBtn.visibility = View.INVISIBLE
                mRejectBtn.isClickable = false
                mTrackingImage.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Get ActiveTrack RectF
     *
     * @param iv
     * @return
     */
    private fun getActiveTrackRect(iv: View): RectF {
        val parent = iv.parent as View
        return RectF(
            (iv.left.toFloat() + iv.x) / parent.width.toFloat(),
            (iv.top.toFloat() + iv.y) / parent.height.toFloat(),
            (iv.right.toFloat() + iv.x) / parent.width.toFloat(),
            (iv.bottom.toFloat() + iv.y) / parent.height.toFloat()
        )
    }

    /**
     * Post Result RectF
     *
     * @param iv
     * @param rectF
     * @param targetState
     */
    private fun postResultRect(
        iv: ImageView, rectF: RectF,
        targetState: ActiveTrackTargetState
    ) {
        val parent = iv.parent as View
        val l =
            ((rectF.centerX() - rectF.width() / 2) * parent.width).toInt()
        val t =
            ((rectF.centerY() - rectF.height() / 2) * parent.height).toInt()
        val r =
            ((rectF.centerX() + rectF.width() / 2) * parent.width).toInt()
        val b =
            ((rectF.centerY() + rectF.height() / 2) * parent.height).toInt()
        runOnUiThread {
            mTrackingImage.visibility = View.VISIBLE
            if (targetState == ActiveTrackTargetState.CANNOT_CONFIRM
                || targetState == ActiveTrackTargetState.UNKNOWN
            ) {
                iv.setImageResource(R.drawable.visual_track_cannotconfirm)
            } else if (targetState == ActiveTrackTargetState.WAITING_FOR_CONFIRMATION) {
                iv.setImageResource(R.drawable.visual_track_needconfirm)
            } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_LOW_CONFIDENCE) {
                iv.setImageResource(R.drawable.visual_track_lowconfidence)
            } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_HIGH_CONFIDENCE) {
                iv.setImageResource(R.drawable.visual_track_highconfidence)
            }
            iv.x = l.toFloat()
            iv.y = t.toFloat()
            iv.layoutParams.width = r - l
            iv.layoutParams.height = b - t
            iv.requestLayout()
        }
    }

    /**
     * PostMultiResult
     *
     * @param iv
     * @param rectF
     * @param information
     */
    private fun postMultiResultRect(
        iv: MultiTrackingView?, rectF: RectF,
        information: SubjectSensingState
    ) {
        val parent = iv?.parent as View
        val l =
            ((rectF.centerX() - rectF.width() / 2) * parent.width).toInt()
        val t =
            ((rectF.centerY() - rectF.height() / 2) * parent.height).toInt()
        val r =
            ((rectF.centerX() + rectF.width() / 2) * parent.width).toInt()
        val b =
            ((rectF.centerY() + rectF.height() / 2) * parent.height).toInt()
        runOnUiThread {
            mTrackingImage.visibility = View.INVISIBLE
            iv.x = l.toFloat()
            iv.y = t.toFloat()
            iv.layoutParams.width = r - l
            iv.layoutParams.height = b - t
            iv.requestLayout()
            iv.updateView(information)
        }
    }

    /**
     * Update MultiTrackingView
     *
     * @param targetSensingInformations
     */
    private fun updateMultiTrackingView(targetSensingInformations: Array<SubjectSensingState>) {
        val indexs = ArrayList<Int>()
        for (target in targetSensingInformations) {
            indexs.add(target.index)
            if (targetViewHashMap.containsKey(target.index)) {
                val targetView: MultiTrackingView? = targetViewHashMap[target.index]
                postMultiResultRect(targetView, target.targetRect, target)
            } else {
                val trackingView = MultiTrackingView(this@TrackingTestActivity)
                mBgLayout.addView(trackingView, layoutParams)
                targetViewHashMap[target.index] = trackingView
            }
        }
        val missingIndexs = ArrayList<Int>()
        for (key in targetViewHashMap.keys) {
            var isDisappeared = true
            for (index in indexs) {
                if (index == key) {
                    isDisappeared = false
                    break
                }
            }
            if (isDisappeared) {
                missingIndexs.add(key)
            }
        }
        for (i in missingIndexs) {
            val view: MultiTrackingView? = targetViewHashMap.remove(i)
            mBgLayout.removeView(view)
        }
    }


    /**
     * Enable MultiTracking
     *
     * @param isChecked
     */
    private fun setAutoSensingEnabled(isChecked: Boolean) {
        if (mActiveTrackOperator != null) {
            if (isChecked) {
                startMode = ActiveTrackMode.TRACE
                mActiveTrackOperator?.enableAutoSensing(CommonCallbacks.CompletionCallback { error: DJIError ->
                    runOnUiThread { mAutoSensingSw.isChecked = !isChecked }
                    setResultToToast("Set AutoSensing Enabled " + error.description)
                })
            } else {
                disableAutoSensing()
            }
        }
    }

    /**
     * Enable QuickShotMode
     *
     * @param isChecked
     */
    private fun setAutoSensingForQuickShotEnabled(isChecked: Boolean) {
        if (mActiveTrackOperator != null) {
            if (isChecked) {
                mActiveTrackOperator?.enableAutoSensingForQuickShot(CommonCallbacks.CompletionCallback { error: DJIError ->
                    runOnUiThread { mQuickShotSw.isChecked = !isChecked }
                    setResultToToast("Set QuickShot Enabled " + error.description)
                })
            } else {
                disableAutoSensing()
            }
        }
    }

    /**
     * Disable AutoSensing
     */
    private fun disableAutoSensing() {
        if (mActiveTrackOperator != null) {
            mActiveTrackOperator?.disableAutoSensing(CommonCallbacks.CompletionCallback { error: DJIError ->
                setResultToToast(error.description)
            })
        }
    }


    /**
     * Confim Mission by Index
     */
    private fun startAutoSensingMission() {
        if (trackingIndex != TrackingTestActivity.INVALID_INDEX) {
            val mission = ActiveTrackMission(null, startMode)
            mission.quickShotMode = quickShotMode
            mission.targetIndex = trackingIndex
            mActiveTrackOperator?.startAutoSensingMission(
                mission
            ) { error ->
                if (error == null) {
                    setResultToToast("Accept Confim index: $trackingIndex Success!")
                    trackingIndex = TrackingTestActivity.INVALID_INDEX
                } else {
                    setResultToToast(error.description)
                }
            }
        }
    }


    /**
     * Change Storage Location
     */
    private fun switchStorageLocation(storageLocation: StorageLocation) {
        val keyManager = KeyManager.getInstance()
        val storageLoactionkey: DJIKey = CameraKey.create(
            CameraKey.CAMERA_STORAGE_LOCATION,
            MAIN_CAMERA_INDEX
        )
        if (storageLocation == StorageLocation.INTERNAL_STORAGE) {
            keyManager.setValue(
                storageLoactionkey,
                StorageLocation.SDCARD,
                object : SetCallback {
                    override fun onSuccess() {
                        setResultToToast("Change to SD card Success!")
                    }

                    override fun onFailure(error: DJIError) {
                        setResultToToast(error.description)
                    }
                })
        } else {
            keyManager.setValue(
                storageLoactionkey,
                StorageLocation.INTERNAL_STORAGE,
                object : SetCallback {
                    override fun onSuccess() {
                        setResultToToast("Change to Interal Storage Success!")
                    }

                    override fun onFailure(error: DJIError) {
                        setResultToToast(error.description)
                    }
                })
        }
    }

    /**
     * determine SD Card is or not Ready
     *
     * @param index
     * @return
     */
    private fun isSDCardReady(index: Int): Boolean {
        val keyManager = KeyManager.getInstance()
        return if (keyManager != null) {
            if (keyManager.getValue(
                    (CameraKey.create(
                        CameraKey.SDCARD_IS_INSERTED,
                        index
                    ))
                ) != null
            ) {
                (keyManager.getValue(
                    (CameraKey.create(
                        CameraKey.SDCARD_IS_INSERTED,
                        index
                    ))
                ) as Boolean
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_IS_INITIALIZING,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_IS_READ_ONLY,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_HAS_ERROR,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_IS_FULL,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_IS_BUSY,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_IS_FORMATTING,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_IS_INVALID_FORMAT,
                        index
                    )
                ) as Boolean)
                        && (keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_IS_VERIFIED,
                        index
                    )
                ) as Boolean)
                        && keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT,
                        index
                    )
                ) as Long > 0
                        && keyManager.getValue(
                    CameraKey.create(
                        CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS,
                        index
                    )
                ) as Int > 0)
            } else {
                false
            }

        } else {
            false
        }

    }

    /**
     * determine Interal Storage is or not Ready
     *
     * @param index
     * @return
     */
    private fun isInteralStorageReady(index: Int): Boolean {
        val keyManager = KeyManager.getInstance()
        val isInternalSupported = keyManager.getValue(
            CameraKey.create(
                CameraKey.IS_INTERNAL_STORAGE_SUPPORTED,
                index
            )
        )
        return if (isInternalSupported != null) {
            if (isInternalSupported as Boolean) {
                (keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_IS_INSERTED,
                        index
                    )
                ) as Boolean
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_IS_INITIALIZING,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_IS_READ_ONLY,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_HAS_ERROR,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_IS_FULL,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_IS_BUSY,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_IS_FORMATTING,
                        index
                    )
                ) as Boolean)
                        && !(keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_IS_INVALID_FORMAT,
                        index
                    )
                ) as Boolean)
                        && (keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_IS_VERIFIED,
                        index
                    )
                ) as Boolean)
                        && keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT,
                        index
                    )
                ) as Long > 0L
                        && keyManager.getValue(
                    CameraKey.create(
                        CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS,
                        index
                    )
                ) as Int > 0)
            } else false
        } else false

    }

    /**
     * Check Storage States
     */
    private fun checkStorageStates() {
        val keyManager = KeyManager.getInstance()
        val storageLocationkey: DJIKey = CameraKey.create(
            CameraKey.CAMERA_STORAGE_LOCATION,
            MAIN_CAMERA_INDEX
        )
        val storageLocationObj = keyManager.getValue(storageLocationkey)
        var storageLocation =
            StorageLocation.INTERNAL_STORAGE
        if (storageLocationObj is StorageLocation) {
            storageLocation = storageLocationObj
        }
        if (storageLocation == StorageLocation.INTERNAL_STORAGE) {
            if (!isInteralStorageReady(MAIN_CAMERA_INDEX) && isSDCardReady(
                    MAIN_CAMERA_INDEX
                )
            ) {
                switchStorageLocation(StorageLocation.SDCARD)
            }
        }
        if (storageLocation == StorageLocation.SDCARD) {
            if (!isSDCardReady(MAIN_CAMERA_INDEX) && isInteralStorageReady(
                    MAIN_CAMERA_INDEX
                )
            ) {
                switchStorageLocation(StorageLocation.INTERNAL_STORAGE)
            }
        }
        val isRecordingKey: DJIKey =
            CameraKey.create(CameraKey.IS_RECORDING, MAIN_CAMERA_INDEX)
        val isRecording = keyManager.getValue(isRecordingKey)
        if (isRecording is Boolean) {
            if (isRecording) {
                keyManager.performAction(
                    CameraKey.create(
                        CameraKey.STOP_RECORD_VIDEO,
                        MAIN_CAMERA_INDEX
                    ), object : ActionCallback {
                        override fun onSuccess() {
                            setResultToToast("Stop Recording Success!")
                        }

                        override fun onFailure(error: DJIError) {
                            setResultToToast("Stop Recording Fail，Error " + error.description)
                        }
                    })
            }
        }
    }

    /**
     * Clear MultiTracking View
     */
    private fun clearCurrentView() {
        if (!targetViewHashMap.isEmpty()) {
            val it: MutableIterator<Map.Entry<Int, MultiTrackingView>> =
                targetViewHashMap.entries.iterator()
            while (it.hasNext()) {
                val entry: Map.Entry<Int, MultiTrackingView> = it.next()
                val view: MultiTrackingView = entry.value
                it.remove()
                runOnUiThread { mBgLayout.removeView(view) }
            }
        }
    }
}