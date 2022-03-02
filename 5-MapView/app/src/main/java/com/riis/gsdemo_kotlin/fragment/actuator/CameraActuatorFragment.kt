package com.riis.gsdemo_kotlin.fragment.actuator

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.riis.gsdemo_kotlin.R
import com.riis.gsdemo_kotlin.Tools
import dji.common.mission.waypointv2.Action.*
import dji.common.mission.waypointv2.Action.ActionTypes.CameraOperationType

class CameraActuatorFragment : Fragment(), IActuatorCallback {

    private lateinit var rbShootSinglePhoto: RadioButton
    private lateinit var rbStartRecordVideo: RadioButton
    private lateinit var rbStopRecordVideo: RadioButton
    private lateinit var rbFocus: RadioButton
    private lateinit var rbZoom: RadioButton
    private lateinit var radioCameraType: RadioGroup
    private lateinit var etZoom: EditText
    private lateinit var etFocusTargetX: EditText
    private lateinit var etFocusTargetY: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_camera_actuator, container, false)
        initUi(root)
        return root
    }

    private fun initUi(v: View) {
        rbShootSinglePhoto = v.findViewById(R.id.rb_shoot_single_photo)
        rbStartRecordVideo = v.findViewById(R.id.rb_start_record_video)
        rbStopRecordVideo = v.findViewById(R.id.rb_stop_record_video)
        rbFocus = v.findViewById(R.id.rb_focus)
        rbZoom = v.findViewById(R.id.rb_zoom)
        radioCameraType = v.findViewById(R.id.radio_camera_type)
        etZoom = v.findViewById(R.id.et_zoom)
        etFocusTargetX = v.findViewById(R.id.et_focus_target_x)
        etFocusTargetY = v.findViewById(R.id.et_focus_target_y)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        radioCameraType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_shoot_single_photo -> hide(
                    R.id.et_focus_target_x,
                    R.id.et_focus_target_y,
                    R.id.et_zoom
                )
                R.id.rb_start_record_video -> hide(
                    R.id.et_focus_target_x,
                    R.id.et_focus_target_y,
                    R.id.et_zoom
                )
                R.id.rb_stop_record_video -> hide(
                    R.id.et_focus_target_x,
                    R.id.et_focus_target_y,
                    R.id.et_zoom
                )
                R.id.rb_focus -> {
                    hide(R.id.et_zoom)
                    show(R.id.et_focus_target_x, R.id.et_focus_target_y)
                }
                R.id.rb_zoom -> {
                    show(R.id.et_zoom)
                    hide(R.id.et_focus_target_x, R.id.et_focus_target_y)
                }
            }
        }
    }

    private fun hide(vararg ids: Int) {
        for (id in ids) {
            view?.findViewById<View>(id)?.visibility = View.GONE
        }
    }

    private fun show(vararg ids: Int) {
        for (id in ids) {
            view?.findViewById<View>(id)?.visibility = View.VISIBLE
        }
    }

    override fun getActuator(): WaypointActuator {
        val focalLength: Int = Tools.getInt(etZoom.text.toString(), 10)
        val type = type
        val focusParam = WaypointCameraFocusParam.Builder()
            .focusTarget(
                PointF(
                    Tools.getFloat(etFocusTargetX.text.toString(), 0.5f), Tools.getFloat(
                        etFocusTargetY.text.toString(), 0.5f
                    )
                )
            )
            .build()
        val zoomParam = WaypointCameraZoomParam.Builder()
            .setFocalLength(focalLength)
            .build()
        val actuatorParam = WaypointCameraActuatorParam.Builder()
            .setCameraOperationType(type)
            .setFocusParam(focusParam)
            .setZoomParam(zoomParam)
            .build()
        return WaypointActuator.Builder()
            .setActuatorType(ActionTypes.ActionActuatorType.CAMERA)
            .setCameraActuatorParam(actuatorParam)
            .build()
    }

    val type: CameraOperationType
        get() {
            when (radioCameraType!!.checkedRadioButtonId) {
                R.id.rb_shoot_single_photo -> return CameraOperationType.SHOOT_SINGLE_PHOTO
                R.id.rb_start_record_video -> return CameraOperationType.START_RECORD_VIDEO
                R.id.rb_stop_record_video -> return CameraOperationType.STOP_RECORD_VIDEO
                R.id.rb_focus -> return CameraOperationType.FOCUS
                R.id.rb_zoom -> return CameraOperationType.ZOOM
            }
            return CameraOperationType.SHOOT_SINGLE_PHOTO
        }

    companion object {
        fun newInstance(): CameraActuatorFragment {
            return CameraActuatorFragment()
        }
    }
}