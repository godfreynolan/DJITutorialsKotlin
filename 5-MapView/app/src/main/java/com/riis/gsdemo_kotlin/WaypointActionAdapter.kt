package com.riis.gsdemo_kotlin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dji.common.mission.waypointv2.Action.WaypointV2Action

class WaypointActionAdapter(private val data: MutableList<WaypointV2Action>) :
    RecyclerView.Adapter<WaypointActionAdapter.ViewHolder>() {

    fun addItem(action: WaypointV2Action?) { // add items to the list
        if (action == null) {
            return
        }
        data.add(action)
        notifyDataSetChanged()
    }

    fun getData(): List<WaypointV2Action> { // return data
        return data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder { // create the view holder
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_waypoint_action, parent, false)
        return ViewHolder(itemView)
    }


    override fun onBindViewHolder(holder: WaypointActionAdapter.ViewHolder, position: Int) { // bind your data to the view
        holder.setData(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { // set all your data
        private val triggerTv: TextView = itemView.findViewById(R.id.tv_trigger)
        private val actuatorTv: TextView = itemView.findViewById(R.id.tv_actuator)
        private val actionIdTv: TextView = itemView.findViewById(R.id.tv_action_id)

        fun setData(data: WaypointV2Action) {
            if (data.trigger != null) {
                triggerTv.text = "Trigger: " + data.trigger.triggerType.name
            }
            if (data.actuator != null) {
                actuatorTv.text = "Actuator: " + data.actuator.actuatorType.name
            }
            actionIdTv.text = "ActionId:" + data.actionID
        }

    }
}