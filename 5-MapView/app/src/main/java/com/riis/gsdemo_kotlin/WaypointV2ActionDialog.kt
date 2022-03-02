package com.riis.gsdemo_kotlin

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatSpinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.riis.gsdemo_kotlin.fragment.actuator.AircraftActuatorFragment
import com.riis.gsdemo_kotlin.fragment.actuator.CameraActuatorFragment
import com.riis.gsdemo_kotlin.fragment.actuator.GimbalActuatorFragment
import com.riis.gsdemo_kotlin.fragment.actuator.IActuatorCallback
import com.riis.gsdemo_kotlin.fragment.trigger.*
import dji.common.mission.waypointv2.Action.ActionTypes
import dji.common.mission.waypointv2.Action.ActionTypes.ActionActuatorType
import dji.common.mission.waypointv2.Action.ActionTypes.ActionTriggerType
import dji.common.mission.waypointv2.Action.WaypointActuator
import dji.common.mission.waypointv2.Action.WaypointTrigger
import dji.common.mission.waypointv2.Action.WaypointV2Action
import java.util.*

class WaypointV2ActionDialog: DialogFragment(), ITriggerCallback {

    private lateinit var tvTitle: TextView
    private lateinit var viewDivision: View
    private lateinit var rvAddedAction: RecyclerView
    private lateinit var nsvActionDetail: NestedScrollView
    private lateinit var tvOk: TextView
    private lateinit var tvTriggerTitle: TextView
    private lateinit var clTrigger: ConstraintLayout
    private lateinit var spinnerTriggerType: AppCompatSpinner
    private lateinit var flTriggerInfo: FrameLayout
    private lateinit var tvActuatorTitle: TextView
    private lateinit var spinnerActuatorType: AppCompatSpinner
    private lateinit var flActuatorInfo: FrameLayout
    private lateinit var actionAdapter: WaypointActionAdapter
    private lateinit var tvAdd: TextView

    var triggerType: MutableList<String> = mutableListOf()
    var actuatorType: MutableList<String> = mutableListOf()
    var actuatorNames: MutableList<String> = mutableListOf()

    private var associateTriggerFragment: AssociateTriggerFragment? = null
    private var simpleIntervalTriggerFragment: SimpleIntervalTriggerFragment? = null
    private var reachPointTriggerFragment: ReachPointTriggerFragment? = null
    private var trajectoryTriggerFragment: TrajectoryTriggerFragment? = null

    private var aircraftActuatorFragment: AircraftActuatorFragment? = null
    private var cameraActuatorFragment: CameraActuatorFragment? = null
    private var gimbalActuatorFragment: GimbalActuatorFragment? = null

    private var currentTriggerFragment: Fragment? = null
    private var currentActuatorFragment: Fragment? = null

    private var actionCallback: IActionCallback? = null
    var actuatorAdapter: ArrayAdapter<String>? = null

    private var position = 0
    var size = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.dialog_waypoint_v2, container, false)
        initUi(root) // initialize the UI
        initData() // initialize the data
        initView() // initialize the view
        return root
    }

    private fun initUi(v: View) {
        tvTitle = v.findViewById(R.id.tv_title)
        viewDivision = v.findViewById(R.id.view_division)
        rvAddedAction = v.findViewById(R.id.rv_added_action)
        nsvActionDetail = v.findViewById(R.id.nsv_action_detail)
        tvOk = v.findViewById(R.id.tv_ok)
        tvAdd = v.findViewById(R.id.tv_add)
        tvTriggerTitle = v.findViewById(R.id.tv_trigger_title)
        clTrigger = v.findViewById(R.id.cl_trigger)
        spinnerTriggerType = v.findViewById(R.id.spinner_trigger_type)
        flTriggerInfo = v.findViewById(R.id.fl_trigger_info)
        tvActuatorTitle = v.findViewById(R.id.tv_actuator_title)
        spinnerActuatorType = v.findViewById(R.id.spinner_actuator_type)
        flActuatorInfo = v.findViewById(R.id.fl_actuator_info)

        tvOk.setOnClickListener {
            actionCallback?.let {
                it.getActions(actionAdapter.getData())
            }
            dismiss()
            position = 0
        }

        tvAdd.setOnClickListener { // add action
            val trigger = getWaypointTrigger()
            val actuator = getWaypointActuator()
            val result = verifyAction(trigger, actuator)
            if (result) { // add action to the adapter
                val action = WaypointV2Action.Builder()
                        .setTrigger(trigger)
                        .setActuator(actuator)
                        .build()
                actionAdapter.addItem(action)
                updateSize()
            }
        }

    }

    private fun verifyAction(trigger: WaypointTrigger?, actuator: WaypointActuator?): Boolean {
        if (trigger == null || actuator == null) {
            Tools.showToast(requireActivity(), "add fail")
            return false
        }
        if (actuator.actuatorType == ActionActuatorType.GIMBAL
                && actuator.gimbalActuatorParam.operationType == ActionTypes.GimbalOperationType.AIRCRAFT_CONTROL_GIMBAL) {
            if (trigger.triggerType != ActionTriggerType.TRAJECTORY) {
                Tools.showToast(requireActivity(), "this trigger `TRAJECTORY` is one by one with `ActionTypes.GimbalOperationType.AIRCRAFT_CONTROL_GIMBAL`")
                return false
            }
        }
        if (trigger.triggerType == ActionTriggerType.TRAJECTORY) {
            if (actuator.actuatorType != ActionActuatorType.GIMBAL
                    || actuator.gimbalActuatorParam.operationType != ActionTypes.GimbalOperationType.AIRCRAFT_CONTROL_GIMBAL) {
                Tools.showToast(requireActivity(), "this trigger `TRAJECTORY` is one by one with `ActionTypes.GimbalOperationType.AIRCRAFT_CONTROL_GIMBAL`")
                return false
            }
        }
        return true
    }

    private fun updateSize() {
        reachPointTriggerFragment?.size = actionAdapter.itemCount
        associateTriggerFragment?.size = actionAdapter.itemCount
        simpleIntervalTriggerFragment?.size = actionAdapter.itemCount
        trajectoryTriggerFragment?.size = actionAdapter.itemCount

    }

    private fun getWaypointActuator(): WaypointActuator? {
        if (currentActuatorFragment == null) {
            return null
        }
        return if (currentActuatorFragment is IActuatorCallback) {
            (currentActuatorFragment as IActuatorCallback).getActuator()
        } else null
    }

    private fun getWaypointTrigger(): WaypointTrigger? {
        if (currentTriggerFragment == null) {
            return null
        }
        return if (currentTriggerFragment is ITriggerCallback) {
            (currentTriggerFragment as ITriggerCallback).getTrigger()
        } else null
    }

    override fun getTrigger(): WaypointTrigger? {
        return getWaypointTrigger()
    }

    private fun initView() {
        rvAddedAction.layoutManager = LinearLayoutManager(context)
        rvAddedAction.addItemDecoration(DividerItemDecoration(context, LinearLayout.HORIZONTAL))

        actionAdapter = WaypointActionAdapter(mutableListOf())
        rvAddedAction.adapter = actionAdapter

        val triggerAdapter: ArrayAdapter<String> = ArrayAdapter<String>(requireActivity(),
                android.R.layout.simple_spinner_dropdown_item, triggerType)
        spinnerTriggerType.adapter = triggerAdapter
        spinnerTriggerType.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if (position == 0) {
                    hideTriggerFragment()
                    return
                }
                when (ActionTriggerType.valueOf(triggerType[position])) {
                    ActionTriggerType.ASSOCIATE -> {
                        if (associateTriggerFragment == null) {
                            associateTriggerFragment = AssociateTriggerFragment()
                        }
                        associateTriggerFragment?.let { triggerFragment ->
                            triggerFragment.size = actionAdapter.getData().size
                            showFragment(triggerFragment, R.id.fl_trigger_info)
                            currentTriggerFragment = triggerFragment
                        }
                    }
                    ActionTriggerType.SIMPLE_INTERVAL -> {
                        if (simpleIntervalTriggerFragment == null) {
                            simpleIntervalTriggerFragment = SimpleIntervalTriggerFragment.newInstance()
                        }
                        simpleIntervalTriggerFragment?.let { triggerFragment ->
                            triggerFragment.size = size
                            showFragment(triggerFragment, R.id.fl_trigger_info)
                            currentTriggerFragment = triggerFragment
                        }
                    }
                    ActionTriggerType.REACH_POINT -> {
                        if (reachPointTriggerFragment == null) {
                            reachPointTriggerFragment = ReachPointTriggerFragment.newInstance()
                        }
                        reachPointTriggerFragment?.let { triggerFragment ->
                            triggerFragment.size = size
                            showFragment(triggerFragment, R.id.fl_trigger_info)
                            currentTriggerFragment = triggerFragment
                        }
                    }
                    ActionTriggerType.TRAJECTORY -> {
                        if (trajectoryTriggerFragment == null) {
                            trajectoryTriggerFragment = TrajectoryTriggerFragment.newInstance()
                        }
                        trajectoryTriggerFragment?.let { triggerFragmnet ->
                            triggerFragmnet.size = size
                            showFragment(triggerFragmnet, R.id.fl_trigger_info)
                            currentTriggerFragment = triggerFragmnet
                        }
                    }
                    ActionTriggerType.UNKNOWN -> hideTriggerFragment()
                    else -> {}
                }
                hideActuatorFragment()
                changeActuatorAdapter(ActionTriggerType.valueOf(triggerType[position]))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        actuatorAdapter = ArrayAdapter<String>(requireActivity(),
                android.R.layout.simple_spinner_dropdown_item, actuatorType)

        spinnerActuatorType.adapter = actuatorAdapter

        spinnerActuatorType.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if (position == 0) {
                    hideActuatorFragment()
                    return
                }
                when (ActionActuatorType.valueOf(actuatorType[position])) {
                    ActionActuatorType.SPRAY -> {
                        // 暂不支持
                        Tools.showToast(activity!!, "Not Support")
                        hideActuatorFragment()
                    }
                    ActionActuatorType.CAMERA -> {
                        if (cameraActuatorFragment == null) {
                            cameraActuatorFragment = CameraActuatorFragment.newInstance()
                        }
                        showFragment(cameraActuatorFragment, R.id.fl_actuator_info)
                        currentActuatorFragment = cameraActuatorFragment
                    }
                    ActionActuatorType.PLAYLOAD -> {
                        Tools.showToast(requireActivity(), "Not Support")
                        hideActuatorFragment()
                    }
                    ActionActuatorType.GIMBAL -> {
                        if (gimbalActuatorFragment == null) {
                            gimbalActuatorFragment = GimbalActuatorFragment.newInstance(this@WaypointV2ActionDialog)
                        }
                        gimbalActuatorFragment?.let { actuatorFragment ->
                            showFragment(actuatorFragment, R.id.fl_actuator_info)
                            currentActuatorFragment = actuatorFragment
                            actuatorFragment.flush()
                        }
                    }
                    ActionActuatorType.AIRCRAFT_CONTROL -> {
                        if (aircraftActuatorFragment == null) {
                            aircraftActuatorFragment = AircraftActuatorFragment.newInstance()
                        }
                        aircraftActuatorFragment?.let { actuatorFragment ->
                            showFragment(actuatorFragment, R.id.fl_actuator_info)
                            currentActuatorFragment = actuatorFragment
                        }
                    }
                    ActionActuatorType.NAVIGATION -> {
                        Tools.showToast(activity!!, "Not Support")
                        hideActuatorFragment()
                    }
                    ActionActuatorType.UNKNOWN -> hideActuatorFragment()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun changeActuatorAdapter(triggerType: ActionTriggerType) {
        when (triggerType) {
            ActionTriggerType.COMPLEX_REACH_POINTS -> flushActuator()
            ActionTriggerType.ASSOCIATE -> flushActuator()
            ActionTriggerType.SIMPLE_INTERVAL -> flushActuator()
            ActionTriggerType.REACH_POINT -> flushActuator()
            ActionTriggerType.TRAJECTORY -> {
                // this trigger is one by one with ActionTypes.GimbalOperationType#AIRCRAFT_CONTROL_GIMBAL.
                actuatorType.removeAll(actuatorNames)
                actuatorType.add("Please select actuator type")
                actuatorType.add(ActionActuatorType.GIMBAL.name)
            }
            else -> {
            }
        }
    }

    private fun flushActuator() {
        actuatorType.clear()
        actuatorType.addAll(actuatorNames)
        actionAdapter.notifyDataSetChanged()
    }

    private fun hideActuatorFragment() {
        if (currentActuatorFragment == null) {
            return
        }
        val transaction = childFragmentManager.beginTransaction()
        transaction.hide(currentActuatorFragment!!)
        transaction.commit()
    }


    private fun showFragment(fragment: Fragment?, @IdRes id: Int) {
        if (fragment == null || fragment.isVisible) {
            return
        }
        val transaction = childFragmentManager.beginTransaction()
        if (fragment.isAdded) {
            if (fragment is BaseTriggerFragment && currentTriggerFragment != null) {
                transaction.hide(currentTriggerFragment!!)
            } else if (fragment is IActuatorCallback && currentActuatorFragment != null) {
                transaction.hide(currentActuatorFragment!!)
            }
            transaction.show(fragment)
        } else {
            transaction.replace(id, fragment)
        }
        transaction.commit()
    }

    private fun hideTriggerFragment() {
        if (currentTriggerFragment == null) {
            return
        }
        val transaction = childFragmentManager.beginTransaction()
        transaction.hide(currentTriggerFragment!!)
        transaction.commit()
    }

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    private fun initData() {
        triggerType = ArrayList()
        actuatorType = ArrayList()
        triggerType.add("Please select trigger type")
        for (type in ActionTriggerType.values()) {
            if (type == ActionTriggerType.COMPLEX_REACH_POINTS) {
                // not support
                continue
            }
            triggerType.add(type.name)
        }
        actuatorNames = ArrayList<String>()
        actuatorNames.add("Please select actuator type")
        actuatorNames.add(ActionTypes.ActionActuatorType.GIMBAL.name)
        actuatorNames.add(ActionTypes.ActionActuatorType.CAMERA.name)
        actuatorNames.add(ActionTypes.ActionActuatorType.AIRCRAFT_CONTROL.name)
        actuatorType.addAll(actuatorNames)
    }

    fun setActionCallback(actionCallback: IActionCallback) {
        this.actionCallback = actionCallback
    }

    interface IActionCallback {
        fun getActions(actions: List<WaypointV2Action>?)
    }
}