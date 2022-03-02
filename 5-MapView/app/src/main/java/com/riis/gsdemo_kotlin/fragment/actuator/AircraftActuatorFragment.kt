package com.riis.gsdemo_kotlin.fragment.actuator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.riis.gsdemo_kotlin.R
import com.riis.gsdemo_kotlin.Tools
import dji.common.mission.waypointv2.Action.*
import dji.common.mission.waypointv2.Action.ActionTypes.AircraftControlType
import dji.common.mission.waypointv2.WaypointV2MissionTypes

class AircraftActuatorFragment: Fragment(), IActuatorCallback {

    private val type = AircraftControlType.UNKNOWN
    private lateinit var rbRotateYaw: RadioButton
    private lateinit var rbStartStopFly: RadioButton
    private lateinit var radioType: RadioGroup
    private lateinit var boxYawRelative: AppCompatCheckBox
    private lateinit var boxYawClockwise: AppCompatCheckBox
    private lateinit var clYaw: ConstraintLayout
    private lateinit var boxStartStopFly: AppCompatCheckBox
    private lateinit var clStartStop: ConstraintLayout
    private lateinit var yawAngle: TextView

    companion object {
        fun newInstance(): AircraftActuatorFragment {
            return AircraftActuatorFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.fragment_aircraft_actuator, container, false)
        initUi(root)

        radioType.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.rb_rotate_yaw -> {

                }
                R.id.rb_start_stop_fly -> {

                }
            }
        }
        return root
    }

    private fun initUi(v: View) {
        rbRotateYaw = v.findViewById(R.id.rb_rotate_yaw)
        rbStartStopFly = v.findViewById(R.id.rb_start_stop_fly)
        radioType = v.findViewById(R.id.radio_type)
        boxYawRelative = v.findViewById(R.id.box_yaw_relative)
        boxYawClockwise = v.findViewById(R.id.box_yaw_clockwise)
        clYaw = v.findViewById(R.id.cl_yaw)
        boxStartStopFly = v.findViewById(R.id.box_start_stop_fly)
        clStartStop = v.findViewById(R.id.cl_start_stop)
        yawAngle = v.findViewById(R.id.et_yaw_angle)

    }

    override fun getActuator(): WaypointActuator {
        val yaw: Float = Tools.getFloat(yawAngle.text.toString(), 0f)
        val yawParam = WaypointAircraftControlRotateYawParam.Builder()
            .setDirection(if (boxYawClockwise.isChecked) WaypointV2MissionTypes.WaypointV2TurnMode.CLOCKWISE else WaypointV2MissionTypes.WaypointV2TurnMode.COUNTER_CLOCKWISE)
            .setRelative(boxYawRelative.isChecked)
            .setYawAngle(yaw)
            .build()
        val startParam = WaypointAircraftControlStartStopFlyParam.Builder()
            .setStartFly(boxStartStopFly.isChecked)
            .build()
        val controlParam = WaypointAircraftControlParam.Builder()
            .setAircraftControlType(type)
            .setFlyControlParam(startParam)
            .setRotateYawParam(yawParam)
            .build()

        return WaypointActuator.Builder()
            .setActuatorType(ActionTypes.ActionActuatorType.AIRCRAFT_CONTROL)
            .setAircraftControlActuatorParam(controlParam)
            .build()
    }
}