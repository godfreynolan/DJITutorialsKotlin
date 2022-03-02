package com.riis.gsdemo_kotlin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.SupportMapFragment
import dji.common.error.DJIError
import dji.common.error.DJIWaypointV2Error
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.RTKState
import dji.common.mission.waypoint.WaypointMissionFinishedAction
import dji.common.mission.waypoint.WaypointMissionHeadingMode
import dji.common.mission.waypointv2.*
import dji.common.mission.waypointv2.Action.WaypointV2Action
import dji.common.mission.waypointv2.WaypointV2MissionTypes.MissionFinishedAction
import dji.common.mission.waypointv2.WaypointV2MissionTypes.MissionGotoWaypointMode
import dji.common.model.LocationCoordinate2D
import dji.common.util.CommonCallbacks
import dji.sdk.flightcontroller.RTK
import dji.sdk.mission.waypoint.WaypointV2MissionOperator
import dji.sdk.mission.waypoint.WaypointV2MissionOperatorListener
import dji.sdk.sdkmanager.DJISDKManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Waypoint2Activity : AppCompatActivity(), MapboxMap.OnMapClickListener, OnMapReadyCallback, View.OnClickListener, WaypointV2ActionDialog.IActionCallback  {

    private lateinit var locate: Button
    private lateinit var add: Button
    private lateinit var clear: Button
    private lateinit var config: Button
    private lateinit var upload: Button
    private lateinit var start: Button
    private lateinit var stop: Button
    private lateinit var logTv: TextView

    private var isAdd = false
    private var mapboxMap: MapboxMap? = null

    private val mMarkers: MutableMap<Int, Marker> = ConcurrentHashMap() // this will store all the markers in a hashmap
    private var droneMarker: Marker? = null

    private var altitude = 100.0f
    private var mSpeed = 10.0f

    private val waypointList: MutableList<WaypointV2> = mutableListOf()

    var waypointMissionBuilder: WaypointV2Mission.Builder? = null

    private var instance: WaypointV2MissionOperator? = null
    private var finishedAction = MissionFinishedAction.NO_ACTION
    private var headingMode = WaypointMissionHeadingMode.AUTO
    private var firstMode = MissionGotoWaypointMode.SAFELY
    private var actionDialog: WaypointV2ActionDialog? = null
    private var v2Actions: MutableList<WaypointV2Action> = mutableListOf()
    private var canUploadAction = false
    private var canStartMission = false
    private var homeLat = 181.0
    private var homeLng = 181.0
    private var aircraftLat = 181.0
    private var aircraftLng = 181.0
    private val useRTKLocation = false
    private var rtk: RTK? = null
    private var droneHeading = 0f
    private var droneHeight = 0f
    var canUpload = false

    companion object {
        const val TAG = "GSDemoWaypoint2.0"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_waypoint2)

        if (getWaypointMissionOperator() == null) {
            setResultToToast("Not support Waypoint2.0")
            //return
        }

        initUI() // initializes the UI

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map2) as SupportMapFragment
        mapFragment.onCreate(savedInstanceState)
        mapFragment.getMapAsync(this)

        addListener()
        setDroneLocationListener()
    }

    private fun setResultToToast(string: String) { // just a helper for showing new toasts
        runOnUiThread { Toast.makeText(this@Waypoint2Activity, string, Toast.LENGTH_SHORT).show() }
    }

    private fun setDroneLocationListener() { // this will set the listener for the drone position
        val flightController = DJIDemoApplication.getFlightController()

        if (flightController == null) {
            Tools.showToast(this, "FC is null, comeback later!")
            return
        }
        if (useRTKLocation) { // if using rtk location
            if (flightController.rtk.also { rtk = it } == null) {
                Tools.showToast(this, "Not support RTK, use Flyc GPS!")
                flightController.setStateCallback(FlightControllerState.Callback { state ->
                    homeLat = state.homeLocation.latitude
                    homeLng = state.homeLocation.longitude
                    aircraftLat = state.aircraftLocation.latitude
                    aircraftLng = state.aircraftLocation.longitude
                    droneHeading = state.aircraftHeadDirection.toFloat()
                    droneHeight = state.aircraftLocation.altitude
                })
            } else {
                flightController.setStateCallback(FlightControllerState.Callback { state ->
                    homeLat = state.homeLocation.latitude
                    homeLng = state.homeLocation.longitude
                })
                rtk?.setStateCallback(RTKState.Callback { state ->
                    aircraftLat = state.fusionMobileStationLocation.latitude
                    aircraftLng = state.fusionMobileStationLocation.longitude
                    droneHeading = state.fusionHeading
                    droneHeight = state.fusionMobileStationAltitude
                    updateDroneLocation()
                })
            }
        } else {
            if (flightController.rtk.also { rtk = it } != null) {
                flightController.rtk?.setStateCallback(null)
            }
            flightController.setStateCallback(FlightControllerState.Callback { state ->
                homeLat = state.homeLocation.latitude
                homeLng = state.homeLocation.longitude
                aircraftLat = state.aircraftLocation.latitude
                aircraftLng = state.aircraftLocation.longitude
                droneHeading = state.aircraftHeadDirection.toFloat()
                droneHeight = state.aircraftLocation.altitude
                updateDroneLocation()
            })
        }
    }

    private fun initUI() {
        locate = findViewById(R.id.locate)
        add = findViewById(R.id.add)
        clear = findViewById(R.id.clear)
        config = findViewById(R.id.config)
        upload = findViewById(R.id.upload)
        start = findViewById(R.id.start)
        stop = findViewById(R.id.stop)
        logTv = findViewById(R.id.tv_log)
        locate.setOnClickListener(this)
        add.setOnClickListener(this)
        clear.setOnClickListener(this)
        config.setOnClickListener(this)
        upload.setOnClickListener(this)
        start.setOnClickListener(this)
        stop.setOnClickListener(this)


        findViewById<View>(R.id.btn_add_action).setOnClickListener(View.OnClickListener { v -> // set the listener for this button
            if (isAdd) {
                Toast.makeText(v.context, "Adding waypoint!", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (waypointMissionBuilder == null || waypointMissionBuilder?.waypointCount == 0) {
                Toast.makeText(v.context, "Please add waypoint first!", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (actionDialog == null) {
                actionDialog = WaypointV2ActionDialog() // initialize the action dialog
                actionDialog?.setActionCallback(this)
                waypointMissionBuilder?.waypointCount?.let {
                    actionDialog?.size = it
                }
                actionDialog?.show(supportFragmentManager, "add_action") // display the dialog
            }
        })
        findViewById<View>(R.id.btn_upload_action).setOnClickListener(this) // set the click listener
    }

    private fun addListener() {
        getWaypointMissionOperator()?.addWaypointEventListener(eventNotificationListener)
    }

    private fun removeListener() {
        getWaypointMissionOperator()?.removeWaypointListener(eventNotificationListener)
    }

    private val eventNotificationListener: WaypointV2MissionOperatorListener = object : WaypointV2MissionOperatorListener {
        override fun onDownloadUpdate(waypointV2MissionDownloadEvent: WaypointV2MissionDownloadEvent) {}
        override fun onUploadUpdate(uploadEvent: WaypointV2MissionUploadEvent) {
            if (uploadEvent.currentState == WaypointV2MissionState.UPLOADING || uploadEvent.error != null) {
                // deal with the progress or the error info
            }
            if (uploadEvent.currentState == WaypointV2MissionState.READY_TO_EXECUTE) {
                // Can upload actions in it.
                // getWaypointMissionOperator().uploadWaypointActions();
                canUploadAction = true
            }
            if (uploadEvent.previousState == WaypointV2MissionState.UPLOADING
                    && uploadEvent.currentState == WaypointV2MissionState.READY_TO_EXECUTE) {
                // upload complete, can start mission
                // getWaypointMissionOperator().startMission();
                canStartMission = true
            }
            logTv.post { logTv.text = "cur_state:" + uploadEvent.currentState.name }
        }

        override fun onExecutionUpdate(waypointV2MissionExecutionEvent: WaypointV2MissionExecutionEvent) {}
        override fun onExecutionStart() {}
        override fun onExecutionFinish(djiWaypointV2Error: DJIWaypointV2Error) {}
        override fun onExecutionStopped() {}
    }

    override fun getActions(actions: List<WaypointV2Action>?) { // this will get the waypoint actions
        actions?.let {
            v2Actions = it as MutableList<WaypointV2Action>
            Log.d("v2_action", "originSize=${it.size}")
        }
    }

    override fun onDestroy() {
        removeListener()
        super.onDestroy()
    }

    private fun getWaypointMissionOperator(): WaypointV2MissionOperator? { // returns waypoint mission operator
        if (instance == null) { // if not already initialized
            val missionControl = DJISDKManager.getInstance().missionControl
            if (missionControl != null) {
                Log.i(TAG, "isMissionControl Null: False")
                instance = missionControl.waypointMissionV2Operator
                if (instance == null) {
                    Log.i(TAG, "isWaypointMissionV2Operation Null: True")
                }
            } else {
                Log.i(TAG, "isMissionControl Null: TRUE")
            }
        }
        return instance
    }

    override fun onMapClick(point: LatLng): Boolean {
        if (isAdd) { // if adding waypoints
            markWaypoint(point)
            val mWaypoint = WaypointV2.Builder()
                    .setAltitude(altitude.toDouble())
                    .setCoordinate(LocationCoordinate2D(point.latitude, point.longitude))
                    .build()
            //Add Waypoints to Waypoint arraylist;
            waypointMissionBuilder?.let { builder ->
                waypointList.add(mWaypoint)
                builder.addWaypoint(mWaypoint)
            }

            if (waypointMissionBuilder == null) { // initialize the mission builder if not existing already
                waypointMissionBuilder = WaypointV2Mission.Builder()
                waypointList.add(mWaypoint)
                waypointMissionBuilder?.addWaypoint(mWaypoint)
            }

        } else {
            setResultToToast("Cannot Add Waypoint")
        }
        return true
    }

    fun checkGpsCoordination(latitude: Double, longitude: Double): Boolean { // checks for the validity of the gps coordinates
        return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180 && latitude != 0.0 && longitude != 0.0
    }

    // Update the drone location based on states from MCU.
    private fun updateDroneLocation() {
        val pos = LatLng(aircraftLat, aircraftLng)
        //Create MarkerOptions object
        val markerOptions = MarkerOptions()
                .position(pos)
                .icon(IconFactory.getInstance(this).fromResource(R.drawable.aircraft))
        runOnUiThread { // display the drone on the map
            droneMarker?.remove()
            if (checkGpsCoordination(aircraftLat, aircraftLng)) {
                droneMarker = mapboxMap?.addMarker(markerOptions)
            }
        }
    }

    private fun markWaypoint(point: LatLng) {
        //Create MarkerOptions object
        val markerOptions = MarkerOptions()
                .position(point)

        val marker = mapboxMap?.addMarker(markerOptions) // add the markers to the map
        marker?.let {
            mMarkers[mMarkers.size] = it
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.addOnMapClickListener(this)
        //mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(18.0))
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { // set the view for the map

        }
    }

    // the following is commented in the Waypoint1Activity
    override fun onClick(v: View?) { // complete all on click events
        when (v!!.id) {
            R.id.locate -> {
                updateDroneLocation()
                cameraUpdate() // Locate the drone's place
            }
            R.id.add -> {
                enableDisableAdd()
            }
            R.id.clear -> {
                runOnUiThread {
                    mapboxMap?.clear()
                    if (waypointList.isNotEmpty()) {
                        waypointList.clear()
                    }
                    if (v2Actions.isNotEmpty()) {
                        v2Actions.clear()
                    }
                }
                waypointMissionBuilder = null
                updateDroneLocation()
            }
            R.id.config -> {
                showSettingDialog()
            }
            R.id.upload -> {
                uploadWayPointMission()
            }
            R.id.start -> {
                startWaypointMission()
            }
            R.id.stop -> {
                stopWaypointMission()
            }
            R.id.btn_upload_action -> {
                if (!canUploadAction) {
                    setResultToToast("Can`t Upload action")
                    return
                }
                if (v2Actions.isEmpty()) {
                    setResultToToast("Please Add Actions")
                    return
                }
                getWaypointMissionOperator()!!.uploadWaypointActions(v2Actions) { djiWaypointV2Error ->
                    if (djiWaypointV2Error == null) {
                        setResultToToast("Upload action success")
                    } else {
                        setResultToToast("Upload action fail:" + djiWaypointV2Error.description)
                    }
                }
            }
            else -> {}
        }
    }

    private fun cameraUpdate() {
        val pos = LatLng(aircraftLat, aircraftLng)
        val zoomLevel = 18.0
        val cu = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
        mapboxMap?.moveCamera(cu)
    }

    private fun enableDisableAdd() {
        if (!isAdd) {
            isAdd = true
            add.text = "Exit"
        } else {
            isAdd = false
            add.text = "Add"
        }
    }

    private fun showSettingDialog() {
        val wayPointSettings = layoutInflater.inflate(R.layout.dialog_waypoint2setting, null) as LinearLayout
        val wpAltitudeTV = wayPointSettings.findViewById<View>(R.id.altitude) as TextView
        val speedRG = wayPointSettings.findViewById<View>(R.id.speed) as RadioGroup
        val actionAfterFinishedRG = wayPointSettings.findViewById<View>(R.id.actionAfterFinished) as RadioGroup
        val headingRG = wayPointSettings.findViewById<View>(R.id.heading) as RadioGroup
        val firstModeRg = wayPointSettings.findViewById<RadioGroup>(R.id.go_to_first_mode)

        firstModeRg.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_p2p -> firstMode = MissionGotoWaypointMode.POINT_TO_POINT
                R.id.rb_safely -> firstMode = MissionGotoWaypointMode.SAFELY
            }
        }

        speedRG.setOnCheckedChangeListener { _, checkedId ->
            Log.d(Waypoint1Activity.TAG, "Select speed")
            when (checkedId) {
                R.id.lowSpeed -> {
                    mSpeed = 3.0f
                }
                R.id.MidSpeed -> {
                    mSpeed = 5.0f
                }
                R.id.HighSpeed -> {
                    mSpeed = 10.0f
                }
            }
        }

        actionAfterFinishedRG.setOnCheckedChangeListener { _, checkedId ->
            Log.d(Waypoint1Activity.TAG, "Select finish action")

            when (checkedId) {
                R.id.finishNone -> {
                    finishedAction = MissionFinishedAction.NO_ACTION;
                }
                R.id.finishGoHome -> {
                    finishedAction = MissionFinishedAction.GO_HOME
                }
                R.id.finishAutoLanding -> {
                    finishedAction = MissionFinishedAction.AUTO_LAND
                }
                R.id.finishToFirst -> {
                    finishedAction = MissionFinishedAction.GO_FIRST_WAYPOINT
                }
            }
        }

        headingRG.setOnCheckedChangeListener { _, checkedId ->

            Log.d(Waypoint1Activity.TAG, "Select heading")
            when (checkedId) {
                R.id.headingNext -> {
                    headingMode = WaypointMissionHeadingMode.AUTO
                }
                R.id.headingInitDirec -> {
                    headingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION
                }
                R.id.headingRC -> {
                    headingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER
                }
                R.id.headingWP -> {
                    headingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING
                }

            }
        }

        AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish") { _, _ ->
                    val altitudeString = wpAltitudeTV.text.toString()
                    altitude = nullToIntegerDefault(altitudeString).toInt().toFloat()
                    Log.e(TAG, "altitude $altitude")
                    Log.e(TAG, "speed $mSpeed")
                    Log.e(TAG, "mFinishedAction $finishedAction")
                    Log.e(TAG, "mHeadingMode $headingMode")
                    configWayPointMission()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .create()
                .show()
    }

    private fun configWayPointMission() {
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = WaypointV2Mission.Builder()
        }

        waypointMissionBuilder?.let { builder ->
            builder.setFinishedAction(finishedAction)
                    .setMissionID(Random().nextInt(65535).toLong())
                    .setFinishedAction(finishedAction)
                    .setGotoFirstWaypointMode(firstMode)
                    .setMaxFlightSpeed(mSpeed)
                    .autoFlightSpeed = mSpeed

            getWaypointMissionOperator()?.loadMission(builder.build()) { error ->
                if (error == null) {
                    setResultToToast("loadWaypoint succeeded")
                    canUpload = true
                } else {
                    setResultToToast("loadWaypoint failed " + error.description)
                }
            }
        }
    }

    private fun nullToIntegerDefault(value: String): String {
        var newValue = value
        if (!isIntValue(newValue)) newValue = "0"
        return newValue
    }

    private fun isIntValue(value: String): Boolean {
        try {
            var newValue = value.replace(" ", "")
            newValue.toInt()
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun uploadWayPointMission() {
        if (!canUpload) {
            Toast.makeText(this, "please click 'CONFIG' button first", Toast.LENGTH_SHORT).show()
            return
        }
        getWaypointMissionOperator()?.uploadMission { error ->
            if (error == null) {
                setResultToToast("Mission upload successfully!")
            } else {
                setResultToToast("Mission upload failed, error: " + error.description)
            }
        }
    }

    private fun startWaypointMission() {
        getWaypointMissionOperator()?.startMission { error ->
            setResultToToast("Mission Start: " + if (error == null) "Successfully" else error.description)
        }
    }

    private fun stopWaypointMission() {
        getWaypointMissionOperator()?.stopMission { error ->
            setResultToToast("Mission Stop: " + if (error == null) "Successfully" else error.description)
        }
    }
}