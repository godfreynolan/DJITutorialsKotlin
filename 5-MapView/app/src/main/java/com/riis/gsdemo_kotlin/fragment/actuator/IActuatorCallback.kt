package com.riis.gsdemo_kotlin.fragment.actuator

import dji.common.mission.waypointv2.Action.WaypointActuator

interface IActuatorCallback {
    fun getActuator(): WaypointActuator
}