package com.riis.gsdemo_kotlin.fragment.trigger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatRadioButton
import com.riis.gsdemo_kotlin.R
import com.riis.gsdemo_kotlin.Tools
import dji.common.mission.waypointv2.Action.ActionTypes
import dji.common.mission.waypointv2.Action.ActionTypes.ActionIntervalType
import dji.common.mission.waypointv2.Action.WaypointIntervalTriggerParam
import dji.common.mission.waypointv2.Action.WaypointTrigger

class SimpleIntervalTriggerFragment: BaseTriggerFragment(), ITriggerCallback {

    private lateinit var etStartIndex: EditText
    private lateinit var radioGroupType: RadioGroup
    private lateinit var etValue: EditText
    private lateinit var rbDistance: AppCompatRadioButton
    private lateinit var rbTime: AppCompatRadioButton


    companion object {
        fun newInstance(): SimpleIntervalTriggerFragment {
            return SimpleIntervalTriggerFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_simple_interval_trigger, container, false)
        initUi(root)
        return root
    }

    private fun initUi(v: View) {
        etStartIndex = v.findViewById(R.id.et_start_index)
        radioGroupType = v.findViewById(R.id.radio_group_type)
        etValue = v.findViewById(R.id.et_value)
        rbDistance = v.findViewById(R.id.rb_distance)
        rbTime = v.findViewById(R.id.rb_time)
    }

    override fun getTrigger(): WaypointTrigger? {
        val value: Float = Tools.getFloat(etValue!!.text.toString(), 1.1f)
        val start: Int = Tools.getInt(etStartIndex!!.text.toString(), 1)
        size?.let { safeSize ->
            if (start > safeSize) {
                Tools.showToast(requireActivity(), "start can`t bigger waypoint mission size, size=$size")
                return null
            }
        }
        val type =
            if (rbDistance.isChecked) ActionIntervalType.DISTANCE else ActionIntervalType.TIME
        val param = WaypointIntervalTriggerParam.Builder()
            .setStartIndex(start)
            .setInterval(value)
            .setType(type)
            .build()
        return WaypointTrigger.Builder()
            .setTriggerType(ActionTypes.ActionTriggerType.SIMPLE_INTERVAL)
            .setIntervalTriggerParam(param)
            .build()
    }

}