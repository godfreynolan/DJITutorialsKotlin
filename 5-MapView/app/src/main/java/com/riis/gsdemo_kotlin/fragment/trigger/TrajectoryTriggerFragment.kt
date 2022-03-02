package com.riis.gsdemo_kotlin.fragment.trigger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.riis.gsdemo_kotlin.R
import com.riis.gsdemo_kotlin.Tools
import dji.common.mission.waypointv2.Action.ActionTypes
import dji.common.mission.waypointv2.Action.WaypointTrajectoryTriggerParam
import dji.common.mission.waypointv2.Action.WaypointTrigger

class TrajectoryTriggerFragment : BaseTriggerFragment(), ITriggerCallback {

    private lateinit var etStartIndex: EditText
    private lateinit var etEndIndex: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.fragment_trajectory_trigger, container, false)
        initUi(root)
        return root
    }

    private fun initUi(v: View) {
        etStartIndex = v.findViewById(R.id.et_start_index)
        etEndIndex = v.findViewById(R.id.et_end_index)
    }

    override fun getTrigger(): WaypointTrigger {
        val start: Int = Tools.getInt(etStartIndex.text.toString(), 1)
        val end: Int = Tools.getInt(etEndIndex.text.toString(), 1)
        val param = WaypointTrajectoryTriggerParam.Builder()
            .setEndIndex(end)
            .setStartIndex(start)
            .build()
        return WaypointTrigger.Builder()
            .setTriggerType(ActionTypes.ActionTriggerType.TRAJECTORY)
            .setTrajectoryParam(param)
            .build()
    }

    companion object {
        fun newInstance(): TrajectoryTriggerFragment {
            return TrajectoryTriggerFragment()
        }
    }
}