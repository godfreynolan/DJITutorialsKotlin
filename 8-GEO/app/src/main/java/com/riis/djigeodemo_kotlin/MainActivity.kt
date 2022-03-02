package com.riis.djigeodemo_kotlin

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.PolygonOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions
import com.mapbox.mapboxsdk.plugins.annotation.FillManager
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.utils.ColorUtils.colorToRgbaString
import dji.common.error.DJIError
import dji.common.flightcontroller.flyzone.*
import dji.common.model.LocationCoordinate2D
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.log.DJILog
import dji.sdk.base.BaseProduct
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.useraccount.UserAccountManager
import java.util.*
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin


class MainActivity : AppCompatActivity(), MapboxMap.OnMapClickListener, OnMapReadyCallback,
    View.OnClickListener {

    companion object {
        const val TAG = "GEODemoActivity"
    }

    private var mapboxMap: MapboxMap? = null
    private val unlockableIds = mutableListOf<Int>()
    private var marker: Marker? = null
    private val markerOptions = MarkerOptions()
    private var latLng: LatLng? = null
    private var droneLocationLat = 181.0
    private var droneLocationLng = 181.0
    private var customUnlockZones = mutableListOf<CustomUnlockZone>()
    private val flyZoneIdsToUnlock = mutableListOf<Int>()
    private val painter: FlyfrBasePainter = FlyfrBasePainter()
    private var isMapReady = false
    private lateinit var mapView: MapView

    private lateinit var mConnectStatusTextView: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnLogout: Button
    private lateinit var btnUnlock: Button
    private lateinit var btnGetUnlock: Button
    private lateinit var btnGetSurroundNFZ: Button
    private lateinit var btnUpdateLocation: Button
    private lateinit var btnLoadCustomUnlockZones: Button
    private lateinit var btnGetCustomUnlockZones: Button
    private lateinit var btnEnableCustomUnlockZone: Button
    private lateinit var btnDisableCustomUnlockZone: Button
    private lateinit var btnGetEnabledCustomUnlockZone: Button
    private lateinit var btnRefreshLicense: Button
    private lateinit var btnUploadToAircraft: Button
    private lateinit var btnGetCachedLicense: Button
    private lateinit var loginStatusTv: TextView
    private lateinit var flyZonesTv: TextView

    private lateinit var circleManager: CircleManager
    private lateinit var fillManager: FillManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_main)
        initUi()

        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        DJISDKManager.getInstance().flyZoneManager
            .setFlyZoneStateCallback {
                showToast(it.name)
            }
        markerOptions.icon(IconFactory.getInstance(this).fromResource(R.drawable.aircraft))
    }

    private fun initUi() {
        mConnectStatusTextView = findViewById(R.id.ConnectStatusTextView)

        btnLogin = findViewById(R.id.geo_login_btn)
        btnLogout = findViewById(R.id.geo_logout_btn)
        btnUnlock = findViewById(R.id.geo_unlock_nfzs_btn)
        btnGetUnlock = findViewById(R.id.geo_get_unlock_nfzs_btn)
        btnGetSurroundNFZ = findViewById(R.id.geo_get_surrounding_nfz_btn)
        btnUpdateLocation = findViewById(R.id.geo_update_location_btn)
        btnLoadCustomUnlockZones = findViewById(R.id.geo_load_custom_unlock_zones)
        btnGetCustomUnlockZones = findViewById(R.id.geo_get_custom_unlock_zones)
        btnEnableCustomUnlockZone = findViewById(R.id.geo_enable_custom_unlock_zone)
        btnDisableCustomUnlockZone = findViewById(R.id.geo_disable_custom_unlock_zone)
        btnGetEnabledCustomUnlockZone = findViewById(R.id.geo_get_enabled_custom_unlock_zone)
        btnRefreshLicense = findViewById(R.id.geo_reload_unlocked_zone_groups_from_server)
        btnUploadToAircraft = findViewById(R.id.geo_sync_unlocked_zone_group_to_aircraft)
        btnGetCachedLicense = findViewById(R.id.geo_get_loaded_unlocked_zone_groups)

        loginStatusTv = findViewById(R.id.login_status)
        loginStatusTv.setTextColor(Color.BLACK)
        flyZonesTv = findViewById(R.id.fly_zone_tv)
        flyZonesTv.setTextColor(Color.BLACK)

        btnLogin.setOnClickListener(this)
        btnLogout.setOnClickListener(this)
        btnUnlock.setOnClickListener(this)
        btnGetUnlock.setOnClickListener(this)
        btnGetSurroundNFZ.setOnClickListener(this)
        btnUpdateLocation.setOnClickListener(this)
        btnLoadCustomUnlockZones.setOnClickListener(this)
        btnGetCustomUnlockZones.setOnClickListener(this)
        btnEnableCustomUnlockZone.setOnClickListener(this)
        btnDisableCustomUnlockZone.setOnClickListener(this)
        btnGetEnabledCustomUnlockZone.setOnClickListener(this)
        btnRefreshLicense.setOnClickListener(this)
        btnUploadToAircraft.setOnClickListener(this)
        btnGetCachedLicense.setOnClickListener(this)
    }

    override fun onMapClick(point: LatLng): Boolean {
        return false
    }

    private fun loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
            object : CompletionCallbackWith<UserAccountState> {
                override fun onSuccess(userAccountState: UserAccountState) {
                    showToast("Login Success: " + userAccountState.name)
                    runOnUiThread { loginStatusTv.text = userAccountState.name }
                }

                override fun onFailure(error: DJIError) {
                    showToast("Login Error: " + error.description)
                }
            })
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        isMapReady = true
        latLng?.let {
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLng(it))
        }
        mapboxMap.addOnMapClickListener(this)
        mapboxMap.animateCamera(CameraUpdateFactory.zoomTo(17.0))
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            enableLocationComponent(it)
            circleManager = CircleManager(mapView, mapboxMap, it)
            fillManager = FillManager(mapView, mapboxMap, it)

        }
        runOnUiThread {
            val camPos = CameraPosition.Builder()
                .target(LatLng(droneLocationLat, droneLocationLng))
                .zoom(15.0)
                .build()
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos))
        }
        printSurroundFlyZones()
    }

    private fun printSurroundFlyZones() {
        DJISDKManager.getInstance().flyZoneManager.getFlyZonesInSurroundingArea(object :
            CompletionCallbackWith<ArrayList<FlyZoneInformation?>?> {
            override fun onSuccess(flyZones: ArrayList<FlyZoneInformation?>?) {
                showToast("get surrounding Fly Zone Success!")
                updateFlyZonesOnTheMap(flyZones)
                showSurroundFlyZonesInTv(flyZones)
            }

            override fun onFailure(error: DJIError) {
                showToast(error.description)
            }
        })
    }

    private fun showSurroundFlyZonesInTv(flyZones: List<FlyZoneInformation?>?) {
        val sb = StringBuffer()
        if (flyZones != null) {
            for (flyZone in flyZones) {
                if (flyZone != null && flyZone.category != null) {
                    sb.append("FlyZoneId: ").append(flyZone.flyZoneID).append("\n")
                    sb.append("Category: ").append(flyZone.category.name).append("\n")
                    sb.append("Latitude: ").append(flyZone.coordinate.latitude).append("\n")
                    sb.append("Longitude: ").append(flyZone.coordinate.longitude).append("\n")
                    sb.append("FlyZoneType: ").append(flyZone.flyZoneType.name).append("\n")
                    sb.append("Radius: ").append(flyZone.radius).append("\n")
                    if (flyZone.shape != null) {
                        sb.append("Shape: ").append(flyZone.shape.name).append("\n")
                    }
                    sb.append("StartTime: ").append(flyZone.startTime).append("\n")
                    sb.append("EndTime: ").append(flyZone.endTime).append("\n")
                    sb.append("UnlockStartTime: ").append(flyZone.unlockStartTime).append("\n")
                    sb.append("UnlockEndTime: ").append(flyZone.unlockEndTime).append("\n")
                    sb.append("Name: ").append(flyZone.name).append("\n")
                    sb.append("\n")
                }
            }
        }
        runOnUiThread { flyZonesTv.text = sb.toString() }
    }

    private fun updateFlyZonesOnTheMap(flyZones: ArrayList<FlyZoneInformation?>?) {
        mapboxMap?.let { map ->
            runOnUiThread {
                map.clear()
                fillManager.deleteAll()
                if (latLng != null) {
                    marker = map.addMarker(markerOptions.position(latLng))
                }
                flyZones?.let { zones ->
                    for (flyZone in zones) {
                        //print polygon
                        if (flyZone?.subFlyZones != null) {
                            val polygonItems = flyZone.subFlyZones
                            val itemSize = polygonItems.size
                            for (i in 0 until itemSize) {
                                if (polygonItems[i].shape == SubFlyZoneShape.POLYGON) {
                                    DJILog.d(
                                        "updateFlyZonesOnTheMap",
                                        "sub polygon points " + i + " size: " + polygonItems[i].vertices.size
                                    )
                                    DJILog.d(
                                        "updateFlyZonesOnTheMap",
                                        "sub polygon points " + i + " category: " + flyZone.category.value()
                                    )
                                    DJILog.d(
                                        "updateFlyZonesOnTheMap",
                                        "sub polygon points " + i + " limit height: " + polygonItems[i].maxFlightHeight
                                    )
                                    addPolygonMarker(
                                        polygonItems[i].vertices,
                                        flyZone.category,
                                        polygonItems[i].maxFlightHeight
                                    )
                                } else if (polygonItems[i].shape == SubFlyZoneShape.CYLINDER) {
                                    val tmpPos = polygonItems[i].center
                                    val subRadius = polygonItems[i].radius
                                    DJILog.d(
                                        "updateFlyZonesOnTheMap",
                                        "sub circle points " + i + " coordinate: " + tmpPos.latitude + "," + tmpPos.longitude
                                    )
                                    DJILog.d(
                                        "updateFlyZonesOnTheMap",
                                        "sub circle points $i radius: $subRadius"
                                    )

                                    val polygonOptions = PolygonOptions()
                                        .addAll(polygonCircleForCoordinate(LatLng(tmpPos.latitude, tmpPos.longitude), subRadius))

                                    when (flyZone.category) {
                                        FlyZoneCategory.WARNING -> polygonOptions.fillColor(Color.parseColor("#00FF007D"))
                                        FlyZoneCategory.ENHANCED_WARNING -> polygonOptions.fillColor(Color.parseColor("#0000FF7D"))
                                        FlyZoneCategory.AUTHORIZATION -> {
                                            polygonOptions.fillColor(Color.parseColor("#ff00007D"))
                                            unlockableIds.add(flyZone.flyZoneID)
                                        }
                                        FlyZoneCategory.RESTRICTED -> polygonOptions.fillColor(Color.parseColor("#ff00007D"))
                                        else -> {
                                        }
                                    }

                                    mapboxMap?.addPolygon(polygonOptions)

                                    /*val circle = CircleOptions()
                                        .withCircleRadius(subRadius.toFloat())
                                        .withLatLng(LatLng(tmpPos.latitude, tmpPos.longitude))
                                        .withCircleOpacity(0.5f)



                                    when (flyZone.category) {
                                        FlyZoneCategory.WARNING -> circle.withCircleColor(colorToRgbaString(Color.GREEN))
                                        FlyZoneCategory.ENHANCED_WARNING -> circle.withCircleColor(colorToRgbaString(Color.BLUE))
                                        FlyZoneCategory.AUTHORIZATION -> {
                                            circle.withCircleColor(colorToRgbaString(Color.YELLOW))
                                            unlockableIds.add(flyZone.flyZoneID)
                                        }
                                        FlyZoneCategory.RESTRICTED -> circle.withCircleColor(colorToRgbaString(Color.RED))
                                        else -> {
                                        }
                                    }

                                    circleManager.create(circle)*/

                                }
                            }
                        } else {
                            flyZone?.let { zone ->

                                val polygonOptions = PolygonOptions()
                                    .addAll(polygonCircleForCoordinate(LatLng(zone.coordinate.latitude, zone.coordinate.longitude), zone.radius))

                                when (flyZone.category) {
                                    FlyZoneCategory.WARNING -> polygonOptions.fillColor(Color.parseColor("#00FF007D"))
                                    FlyZoneCategory.ENHANCED_WARNING -> polygonOptions.fillColor(Color.parseColor("#0000FF7D"))
                                    FlyZoneCategory.AUTHORIZATION -> {
                                        polygonOptions.fillColor(Color.parseColor("#ff00007D"))
                                        unlockableIds.add(flyZone.flyZoneID)
                                    }
                                    FlyZoneCategory.RESTRICTED -> polygonOptions.fillColor(Color.parseColor("#ff00007D"))
                                    else -> {
                                    }
                                }

                                mapboxMap?.addPolygon(polygonOptions)

                                /*val circle = CircleOptions()
                                    .withCircleRadius(zone.radius.toFloat())
                                    .withLatLng(LatLng(zone.coordinate.latitude, zone.coordinate.longitude))
                                    .withCircleOpacity(0.5f)
                                when (zone.category) {
                                    FlyZoneCategory.WARNING -> circle.withCircleColor(colorToRgbaString(Color.GREEN))
                                    FlyZoneCategory.ENHANCED_WARNING -> circle.withCircleStrokeColor(colorToRgbaString(Color.BLUE))
                                    FlyZoneCategory.AUTHORIZATION -> {
                                        circle.withCircleStrokeColor(colorToRgbaString(Color.YELLOW))
                                        unlockableIds.add(flyZone.flyZoneID)
                                    }
                                    FlyZoneCategory.RESTRICTED -> circle.withCircleStrokeColor(colorToRgbaString(Color.RED))
                                    else -> {
                                    }
                                }
                                circleManager.create(circle)*/
                            }
                        }
                    }
                }
            }
        }
    }

    // Function pulled from: https://github.com/mapbox/mapbox-gl-native/issues/2167#issuecomment-200302992
    private fun polygonCircleForCoordinate(
        location: LatLng,
        radius: Double
    ): ArrayList<LatLng>? {
        val degreesBetweenPoints = 8 //45 sides
        val numberOfPoints = floor(360 / degreesBetweenPoints.toDouble()).toInt()
        val distRadians = radius / 6371000.0 // earth radius in meters
        val centerLatRadians = location.latitude * Math.PI / 180
        val centerLonRadians = location.longitude * Math.PI / 180
        val polygons: ArrayList<LatLng> =
            ArrayList() //array to hold all the points
        for (index in 0 until numberOfPoints) {
            val degrees = index * degreesBetweenPoints.toDouble()
            val degreeRadians = degrees * Math.PI / 180
            val pointLatRadians = asin(
                sin(centerLatRadians) * cos(distRadians) + cos(
                    centerLatRadians
                ) * sin(distRadians) * cos(degreeRadians)
            )
            val pointLonRadians = centerLonRadians + Math.atan2(
                sin(degreeRadians) * sin(distRadians) * Math.cos(
                    centerLatRadians
                ),
                cos(distRadians) - sin(centerLatRadians) * Math.sin(
                    pointLatRadians
                )
            )
            val pointLat = pointLatRadians * 180 / Math.PI
            val pointLon = pointLonRadians * 180 / Math.PI
            val point =
                LatLng(pointLat, pointLon)
            polygons.add(point)
        }
        return polygons
    }

    private fun enableLocationComponent(style: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Get an instance of the component
            val locationComponent = mapboxMap?.locationComponent

            // Activate with options
            locationComponent?.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, style).build()
            )

            // Enable to make component visible
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                locationComponent?.isLocationComponentEnabled = true
                return
            }


            // Set the component's camera mode
            locationComponent?.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent?.renderMode = RenderMode.COMPASS
        }
    }

    private fun updateTitleBar() {
        var ret = false
        val product: BaseProduct? = DJIDemoApplication.getProductInstance()
        if (product != null) {
            if (product.isConnected) {
                //The product is connected
                runOnUiThread {
                    mConnectStatusTextView.text = DJIDemoApplication.getProductInstance()?.model.toString() + " Connected"

                }
                ret = true
            } else {
                if (product is Aircraft) {
                    if (product.remoteController != null && product.remoteController.isConnected) {
                        // The product is not connected, but the remote controller is connected
                        runOnUiThread {
                            mConnectStatusTextView.text = "only RC Connected"
                        }
                        ret = true
                    }
                }
            }
        }
        if (!ret) {
            // The product or the remote controller are not connected.
            runOnUiThread { mConnectStatusTextView.text = "Disconnected" }
        }
    }

    private fun showToast(msg: String?) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateTitleBar()
        initFlightController()
    }

    fun onReturn(view: View?) {
        Log.e(TAG, "onReturn")
        finish()
    }

    private fun initFlightController() {
        DJIDemoApplication.getFlightController()?.setStateCallback {
            droneLocationLat = it.aircraftLocation.latitude
            droneLocationLng = it.aircraftLocation.longitude
            updateDroneLocation()
        }
    }

    private fun updateDroneLocation() {
        runOnUiThread {

            marker?.remove()

            if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
                val pos = LatLng(droneLocationLat, droneLocationLng)
                marker = mapboxMap?.addMarker(markerOptions.position(pos))
            }
        }
    }

    private fun checkGpsCoordinates(
        latitude: Double,
        longitude: Double
    ): Boolean {
        return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180 && latitude != 0.0 && longitude != 0.0
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.geo_unlock_nfzs_btn -> unlockNFZs()
            R.id.geo_get_unlock_nfzs_btn -> DJISDKManager.getInstance().flyZoneManager.getUnlockedFlyZonesForAircraft(
                object : CompletionCallbackWith<List<FlyZoneInformation?>?> {
                    override fun onSuccess(flyZoneInformations: List<FlyZoneInformation?>?) {
                        showToast("Get Unlock NFZ success")
                        showSurroundFlyZonesInTv(flyZoneInformations)
                    }

                    override fun onFailure(djiError: DJIError) {
                        showToast("Get Unlock NFZ failed: " + djiError.description)
                    }
                })
            R.id.geo_get_surrounding_nfz_btn -> printSurroundFlyZones()
            R.id.geo_update_location_btn -> {
                latLng = LatLng(droneLocationLat, droneLocationLng)
                marker = mapboxMap?.addMarker(markerOptions.position(latLng))
                latLng?.let {
                    mapboxMap?.moveCamera(CameraUpdateFactory.newLatLng(it))
                }
                mapboxMap?.animateCamera(CameraUpdateFactory.zoomTo(15.0))
            }
            R.id.geo_load_custom_unlock_zones -> DJISDKManager.getInstance().flyZoneManager.reloadUnlockedZoneGroupsFromServer { error ->
                if (error == null) {
                    showToast("refresh successful")
                } else {
                    showToast("refresh failed: " + error.description)
                }
            }
            R.id.geo_reload_unlocked_zone_groups_from_server -> DJISDKManager.getInstance().flyZoneManager.reloadUnlockedZoneGroupsFromServer {
                showToast(
                    "reloadUnlockedZoneGroupsFromServer successful"
                )
            }
            R.id.geo_sync_unlocked_zone_group_to_aircraft -> DJISDKManager.getInstance().flyZoneManager.syncUnlockedZoneGroupToAircraft { error ->
                if (error == null) {
                    showToast("upload to aircraft successful")
                } else {
                    showToast("upload to aircraft failed: " + error.description)
                }
            }
            R.id.geo_get_loaded_unlocked_zone_groups -> DJISDKManager.getInstance().flyZoneManager.getLoadedUnlockedZoneGroups(
                object : CompletionCallbackWith<List<UnlockedZoneGroup>?> {
                    override fun onSuccess(unlockedZoneGroups: List<UnlockedZoneGroup>?) {
                        val cacheZones = StringBuffer("*** LoadedUnlockedZoneGroups ***\n")
                        if (unlockedZoneGroups != null) {
                            for (group in unlockedZoneGroups) {
                                cacheZones.append(
                                    """ == SN: ${group.sn}"""
                                )
                                cacheZones.append(printCustomUnlockZone(group.customUnlockZones))
                                cacheZones.append("\n")
                                cacheZones.append(printFlyZoneInformation(group.selfUnlockedFlyZones))
                                cacheZones.append("\n")
                            }
                        }
                        val zones = cacheZones.toString()
                        runOnUiThread {
                            flyZonesTv.text = zones
                            if (!isMapReady) {
                                flyZonesTv.setTextColor(Color.WHITE)
                            }
                        }
                    }

                    override fun onFailure(error: DJIError) {
                        showToast("getLoadedUnlockedZoneGroups failed : " + error.description)
                    }
                })
            R.id.geo_get_custom_unlock_zones -> DJISDKManager.getInstance().flyZoneManager.getCustomUnlockZonesFromAircraft(
                object : CompletionCallbackWith<List<CustomUnlockZone>> {
                    override fun onSuccess(customUnlockZones: List<CustomUnlockZone>) {
                        showToast("get custom unlock zones successful size: " + customUnlockZones.size)
                        this@MainActivity.customUnlockZones =
                            (customUnlockZones as ArrayList<CustomUnlockZone>)!!
                        val sb = StringBuffer()
                        for (area in customUnlockZones) {
                            if (isMapReady) {
                                runOnUiThread {
                                    val r = area.radius
                                    val lat = area.center.latitude
                                    val lon = area.center.longitude
                                    val circle = CircleOptions()
                                        .withCircleRadius(r.toFloat())
                                        .withLatLng(LatLng(lat, lon))

                                    if (area.isEnabled) {
                                        circle.withCircleStrokeColor(colorToRgbaString(Color.YELLOW))
                                    } else {
                                        circle.withCircleStrokeColor(colorToRgbaString(Color.RED))
                                    }
                                    circleManager.create(circle)
                                }
                            }
                            sb.append("id: ").append(area.id).append("\n")
                            sb.append("name: ").append(area.name).append("\n")
                            sb.append("isEnabled: ").append(area.isEnabled).append("\n")
                            sb.append("isExpired: ").append(area.isExpired).append("\n")
                            sb.append("lat: ").append(area.center.latitude).append("\n")
                            sb.append("lon: ").append(area.center.longitude).append("\n")
                            sb.append("radius: ").append(area.radius).append("\n")
                            sb.append("start time: ").append(area.startTime).append("\n")
                            sb.append("end time: ").append(area.endTime).append("\n")
                        }
                        runOnUiThread { flyZonesTv.text = sb }
                    }

                    override fun onFailure(error: DJIError) {
                        showToast("get custom unlock zones failed: " + error.description)
                    }
                })
            R.id.geo_enable_custom_unlock_zone -> {
                if (customUnlockZones.isEmpty()) {
                    showToast("No custom unlock zones in the aircraft!")
                } else {
                    val names = arrayOfNulls<String>(customUnlockZones.size)
                    var i = 0
                    while (i < customUnlockZones.size) {
                        val customUnlockZone = customUnlockZones[i]
                        names[i] = customUnlockZone.name
                        i++
                    }
                    val numberPicker = NumberPicker(this)
                    numberPicker.minValue = 0
                    numberPicker.maxValue = names.size - 1
                    numberPicker.displayedValues = names
                    val builder = AlertDialog.Builder(this)
                    builder.setView(numberPicker)
                    builder.setTitle("Enable Custom Unlock Zone")
                    builder.setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        val customUnlockZone = customUnlockZones[numberPicker.value]
                        DJISDKManager.getInstance().flyZoneManager.enableCustomUnlockZone(
                            customUnlockZone
                        ) { error ->
                            if (null == error) {
                                showToast("Enable custom unlock zone successfully!")
                            } else {
                                showToast("Enable custom unlock zone failed: " + error.description)
                            }
                        }
                    }
                    builder.show()
                }
                if (customUnlockZones.isEmpty()) {
                    showToast("No custom unlock zones in the aircraft!")
                } else {
                    DJISDKManager.getInstance().flyZoneManager.enableCustomUnlockZone(
                        null
                    ) { error ->
                        if (null == error) {
                            showToast("Disable custom unlock zone successfully!")
                        } else {
                            showToast("Disable custom unlock zone failed: " + error.description)
                        }
                    }
                }
                DJISDKManager.getInstance().flyZoneManager.getEnabledCustomUnlockZone(object :
                    CompletionCallbackWith<CustomUnlockZone?> {
                    override fun onSuccess(customUnlockZone: CustomUnlockZone?) {
                        if (customUnlockZone != null) {
                            showToast("current enabled custom unlock zone is " + customUnlockZone.name)
                        } else {
                            showToast("no enabled custom unlock zone right now")
                        }
                    }

                    override fun onFailure(error: DJIError) {
                        showToast("get enabled custom unlock zone failed: " + error.description)
                    }
                })
            }
            R.id.geo_disable_custom_unlock_zone -> {
                if (customUnlockZones.isEmpty()) {
                    showToast("No custom unlock zones in the aircraft!")
                } else {
                    DJISDKManager.getInstance().flyZoneManager.enableCustomUnlockZone(
                        null
                    ) { error ->
                        if (null == error) {
                            showToast("Disable custom unlock zone successfully!")
                        } else {
                            showToast("Disable custom unlock zone failed: " + error.description)
                        }
                    }
                }
                DJISDKManager.getInstance().flyZoneManager.getEnabledCustomUnlockZone(object :
                    CompletionCallbackWith<CustomUnlockZone?> {
                    override fun onSuccess(customUnlockZone: CustomUnlockZone?) {
                        if (customUnlockZone != null) {
                            showToast("current enabled custom unlock zone is " + customUnlockZone.name)
                        } else {
                            showToast("no enabled custom unlock zone right now")
                        }
                    }

                    override fun onFailure(error: DJIError) {
                        showToast("get enabled custom unlock zone failed: " + error.description)
                    }
                })
            }
            R.id.geo_get_enabled_custom_unlock_zone -> DJISDKManager.getInstance().flyZoneManager.getEnabledCustomUnlockZone(
                object : CompletionCallbackWith<CustomUnlockZone?> {
                    override fun onSuccess(customUnlockZone: CustomUnlockZone?) {
                        if (customUnlockZone != null) {
                            showToast("current enabled custom unlock zone is " + customUnlockZone.name)
                        } else {
                            showToast("no enabled custom unlock zone right now")
                        }
                    }

                    override fun onFailure(error: DJIError) {
                        showToast("get enabled custom unlock zone failed: " + error.description)
                    }
                })
            R.id.geo_login_btn -> {
                loginAccount()
            }
            R.id.geo_logout_btn -> {
                UserAccountManager.getInstance().logoutOfDJIUserAccount { error ->
                    if (null == error) {
                        showToast("Logout Success")
                        runOnUiThread { loginStatusTv.text = "NotLoggedIn" }
                    } else {
                        showToast("Logout Error: " + error.description)
                    }
                }
            }
        }
    }

    private fun addPolygonMarker(
        polygonPoints: List<LocationCoordinate2D>?,
        flyZoneCategory: FlyZoneCategory,
        height: Int
    ) {
        if (polygonPoints == null) {
            return
        }
        val shape = mutableListOf<MutableList<LatLng>>()
        val points = mutableListOf<LatLng>()
        for (point in polygonPoints) {
            points.add(LatLng(point.latitude, point.longitude))
        }
        shape.add(points)

        var fillColor = "#78FF00"
        var fillOpacity = .5f

        if (painter.heightToColor[height] != null) {
            painter.heightToColor[height]?.let {
                fillOpacity = it
            }
        } else if (flyZoneCategory == FlyZoneCategory.AUTHORIZATION) {
            fillColor = "#28FFFF"
        } else if (flyZoneCategory == FlyZoneCategory.ENHANCED_WARNING || flyZoneCategory == FlyZoneCategory.WARNING) {
            fillColor = "#0d1eff"
        }

        val fillOptions = FillOptions()
            .withLatLngs(shape)
            .withFillColor(fillColor)
            .withFillOpacity(fillOpacity)


        fillManager.create(fillOptions)
    }

    private fun unlockNFZs() {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this)
        input.hint = "Enter Fly Zone ID"
        input.inputType = EditorInfo.TYPE_CLASS_NUMBER
        builder.setView(input)
        builder.setTitle("Unlock Fly Zones")
        builder.setItems(
            arrayOf<CharSequence>("Continue", "Unlock", "Cancel")
        ) { dialog, which -> // The 'which' argument contains the index position
            // of the selected item
            when (which) {
                0 -> if (TextUtils.isEmpty(input.text)) {
                    dialog.dismiss()
                } else {
                    val value1 = input.text.toString()
                    flyZoneIdsToUnlock.add(value1.toInt())
                }
                1 -> if (TextUtils.isEmpty(input.text)) {
                    dialog.dismiss()
                } else {
                    val value2 = input.text.toString()
                    flyZoneIdsToUnlock.add(value2.toInt())
                    DJISDKManager.getInstance().flyZoneManager.unlockFlyZones(
                        flyZoneIdsToUnlock as ArrayList<Int>
                    ) { error ->
                        flyZoneIdsToUnlock.clear()
                        if (error == null) {
                            showToast("unlock NFZ Success!")
                        } else {
                            showToast(error.description)
                        }
                    }
                }
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun printCustomUnlockZone(customUnlockZones: List<CustomUnlockZone>?): StringBuffer? {
        val sb = StringBuffer()
        sb.append("== Custom Unlock Zone ==")
        if (customUnlockZones != null) {
            for (area in customUnlockZones) {
                sb.append("license id: " + area.id)
                sb.append("\n")
                sb.append("license name: " + area.name)
                sb.append("\n")
                sb.append("isEnabled: " + area.isEnabled)
                sb.append("\n")
                sb.append("isExpired: " + area.isExpired)
                sb.append("\n")
                sb.append("latitude: " + area.center.latitude)
                sb.append("\n")
                sb.append("longitude: " + area.center.longitude)
                sb.append("\n")
                sb.append("radius: " + area.radius)
                sb.append("\n")
                sb.append("start time: " + area.startTime)
                sb.append("\n")
                sb.append("end time: " + area.endTime)
                sb.append("\n")
            }
        }
        return sb
    }

    private fun printFlyZoneInformation(flyZoneInformations: List<FlyZoneInformation>?): StringBuffer? {
        val sb = StringBuffer()
        sb.append("\n")
        sb.append("== Fly Zone Information ==")
        if (flyZoneInformations != null) {
            for (flyZone in flyZoneInformations) {
                if (flyZone != null) {
                    sb.append("FlyZoneId: ").append(flyZone.flyZoneID).append("\n")
                    sb.append("Category: ").append(flyZone.category.name).append("\n")
                    sb.append("Latitude: ").append(flyZone.coordinate.latitude).append("\n")
                    sb.append("Longitude: ").append(flyZone.coordinate.longitude).append("\n")
                    sb.append("FlyZoneReason: ").append(flyZone.reason.name).append("\n")
                    sb.append("FlyZoneType: ").append(flyZone.flyZoneType.name).append("\n")
                    sb.append("Radius: ").append(flyZone.radius).append("\n")
                    if (flyZone.shape != null) {
                        sb.append("Shape: ").append(flyZone.shape.name).append("\n")
                    }
                    sb.append("StartTime: ").append(flyZone.startTime).append("\n")
                    sb.append("EndTime: ").append(flyZone.endTime).append("\n")
                    sb.append("UnlockStartTime: ").append(flyZone.unlockStartTime).append("\n")
                    sb.append("UnlockEndTime: ").append(flyZone.unlockEndTime).append("\n")
                    sb.append("Name: ").append(flyZone.name).append("\n")
                    sb.append("\n")
                }
            }
        }
        return sb
    }
}