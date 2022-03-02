package com.riis.gsdemo_kotlin.fragment.trigger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.riis.gsdemo_kotlin.R
import com.riis.gsdemo_kotlin.Tools
import dji.common.mission.waypointv2.Action.ActionTypes
import dji.common.mission.waypointv2.Action.WaypointReachPointTriggerParam
import dji.common.mission.waypointv2.Action.WaypointTrigger

class ReachPointTriggerFragment: BaseTriggerFragment(), ITriggerCallback {

    private lateinit var etStartIndex: EditText
    private lateinit var etAutoTerminateCount: EditText

    companion object {
        fun newInstance(): ReachPointTriggerFragment {
            return ReachPointTriggerFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(
            R.layout.fragment_simple_reach_point_trigger,
            container,
            false
        )
        initUi(root)

        return root
    }

    private fun initUi(v: View) {
        etStartIndex = v.findViewById(R.id.et_start_index)
        etAutoTerminateCount = v.findViewById(R.id.et_auto_terminate_count)
    }

    override fun getTrigger(): WaypointTrigger? {
        val start: Int = Tools.getInt(etStartIndex.text.toString(), 1)
        val count: Int = Tools.getInt(etAutoTerminateCount.text.toString(), 1)
        size?.let { safeSize ->
            if (start > safeSize) {
                Tools.showToast(requireActivity(), "start can`t bigger waypoint mission size, size=$size")
                return null
            }
        }
        val param = WaypointReachPointTriggerParam.Builder()
            .setAutoTerminateCount(count)
            .setStartIndex(start)
            .build()
        return WaypointTrigger.Builder()
            .setTriggerType(ActionTypes.ActionTriggerType.REACH_POINT)
            .setReachPointParam(param)
            .build()
    }
}