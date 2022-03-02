package com.riis.gsdemo_kotlin.fragment.actuator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import com.riis.gsdemo_kotlin.R
import com.riis.gsdemo_kotlin.Tools
import com.riis.gsdemo_kotlin.fragment.trigger.ITriggerCallback
import dji.common.gimbal.Rotation
import dji.common.gimbal.RotationMode
import dji.common.mission.waypointv2.Action.ActionTypes
import dji.common.mission.waypointv2.Action.ActionTypes.GimbalOperationType
import dji.common.mission.waypointv2.Action.WaypointActuator
import dji.common.mission.waypointv2.Action.WaypointGimbalActuatorParam

class GimbalActuatorFragment(private val mCallback: ITriggerCallback): Fragment(), IActuatorCallback {

    private var callback: ITriggerCallback = mCallback

    private lateinit var rbRotateGimbal: RadioButton
    private lateinit var rbAircraftControlGimbal: RadioButton
    private lateinit var radioGimbalType: RadioGroup
    private lateinit var etGimbalRoll: EditText
    private lateinit var etGimbalPitch: EditText
    private lateinit var etGimbalYaw: EditText
    private lateinit var etDurationTime: EditText
    private lateinit var boxAbsolute: AppCompatCheckBox
    private lateinit var boxRollIgnore: AppCompatCheckBox
    private lateinit var boxPitchIgnore: AppCompatCheckBox
    private lateinit var boxYawIgnore: AppCompatCheckBox

    companion object {
        fun newInstance(callback: ITriggerCallback): GimbalActuatorFragment {
            return GimbalActuatorFragment(callback)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.fragment_gimbal_actuator, container, false)
        initUi(root)
        flush()
        return root
    }

    private fun initUi(v: View) {
        rbRotateGimbal = v.findViewById(R.id.rb_rotate_gimbal)
        rbAircraftControlGimbal = v.findViewById(R.id.rb_aircraft_control_gimbal)
        radioGimbalType = v.findViewById(R.id.radio_gimbal_type)
        etGimbalRoll = v.findViewById(R.id.et_gimbal_roll)
        etGimbalPitch = v.findViewById(R.id.et_gimbal_pitch)
        etGimbalYaw = v.findViewById(R.id.et_gimbal_yaw)
        etDurationTime = v.findViewById(R.id.et_duration_time)
        boxAbsolute = v.findViewById(R.id.box_absulote)
        boxRollIgnore = v.findViewById(R.id.box_rollIgnore)
        boxPitchIgnore = v.findViewById(R.id.box_pitch_ignore)
        boxYawIgnore = v.findViewById(R.id.box_yaw_igore)

    }

    fun flush() {
        if (context == null) {
            return
        }
        if (callback.getTrigger()?.triggerType == ActionTypes.ActionTriggerType.TRAJECTORY) {
            rbAircraftControlGimbal.visibility = View.VISIBLE
            rbRotateGimbal.visibility = View.GONE
        } else {
            rbAircraftControlGimbal.visibility = View.GONE
            rbRotateGimbal.visibility = View.VISIBLE
        }
    }

    override fun getActuator(): WaypointActuator {
        val roll: Float = Tools.getFloat(etGimbalRoll.text.toString(), 0.1f)
        val pitch: Float = Tools.getFloat(etGimbalPitch.text.toString(), 0.2f)
        val yaw: Float = Tools.getFloat(etGimbalYaw.text.toString(), 0.3f)
        val duration: Int = Tools.getInt(etDurationTime.text.toString(), 10)
        val type = getType()
        val rotationBuilder = Rotation.Builder()
        if (!boxRollIgnore.isChecked) {
            rotationBuilder.roll(roll)
        }
        if (!boxPitchIgnore.isChecked) {
            rotationBuilder.pitch(pitch)
        }
        if (!boxYawIgnore.isChecked) {
            rotationBuilder.yaw(yaw)
        }
        if (boxAbsolute.isChecked) {
            rotationBuilder.mode(RotationMode.ABSOLUTE_ANGLE)
        } else {
            rotationBuilder.mode(RotationMode.RELATIVE_ANGLE)
        }
        rotationBuilder.time(duration.toDouble())
        val actuatorParam = WaypointGimbalActuatorParam.Builder()
            .operationType(type)
            .rotation(rotationBuilder.build())
            .build()
        return WaypointActuator.Builder()
            .setActuatorType(ActionTypes.ActionActuatorType.GIMBAL)
            .setGimbalActuatorParam(actuatorParam)
            .build()
    }

    private fun getType(): GimbalOperationType {
        when (radioGimbalType.checkedRadioButtonId) {
            R.id.rb_rotate_gimbal -> return GimbalOperationType.ROTATE_GIMBAL
            R.id.rb_aircraft_control_gimbal ->                 // Rotates the gimbal. Only valid when the trigger type is `TRAJECTORY`.
                return GimbalOperationType.AIRCRAFT_CONTROL_GIMBAL
        }
        return GimbalOperationType.UNKNOWN
    }

}