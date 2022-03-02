package com.riis.gsdemo_kotlin.fragment.trigger

import dji.common.mission.waypointv2.Action.WaypointTrigger

interface ITriggerCallback {
    fun getTrigger(): WaypointTrigger?
}