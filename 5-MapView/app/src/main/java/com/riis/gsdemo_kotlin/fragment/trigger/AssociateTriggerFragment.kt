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
import dji.common.mission.waypointv2.Action.ActionTypes.AssociatedTimingType
import dji.common.mission.waypointv2.Action.WaypointTrigger
import dji.common.mission.waypointv2.Action.WaypointV2AssociateTriggerParam

class AssociateTriggerFragment: BaseTriggerFragment(), ITriggerCallback {

    private lateinit var etWaitTime: EditText
    private lateinit var radioGroupType: RadioGroup
    private lateinit var rbSync: AppCompatRadioButton
    private lateinit var rbAfter: AppCompatRadioButton
    private lateinit var etActionId: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.fragment_associate_trigger, container, false)
        initUi(root)
        return root
    }

    private fun initUi(v: View) {
        etWaitTime = v.findViewById(R.id.et_wait_time)
        radioGroupType = v.findViewById(R.id.radio_group_type)
        rbSync = v.findViewById(R.id.rb_sync)
        rbAfter = v.findViewById(R.id.rb_after)
        etActionId = v.findViewById(R.id.et_action_id)
    }

    override fun getTrigger(): WaypointTrigger? {
        val waitTime: Float = Tools.getFloat(etWaitTime.text.toString(), 1f)
        val type =
            if (rbSync.isChecked) AssociatedTimingType.SIMULTANEOUSLY else AssociatedTimingType.AFTER_FINISHED
        val actionId: Int = Tools.getInt(etActionId.text.toString(), 1)
        size?.let { sizeSafe ->
            if (actionId > sizeSafe) {
                activity?.let { Tools.showToast(it, "actionId can`t bigger existed action size, size=$size") }
                return null
            }
        }
        val param = WaypointV2AssociateTriggerParam.Builder()
            .setAssociateType(type)
            .setWaitingTime(waitTime)
            .setAssociateActionID(actionId)
            .build()
        return WaypointTrigger.Builder()
            .setTriggerType(ActionTypes.ActionTriggerType.ASSOCIATE)
            .setAssociateParam(param)
            .build()
    }
}