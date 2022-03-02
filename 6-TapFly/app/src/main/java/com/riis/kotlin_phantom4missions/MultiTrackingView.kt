package com.riis.kotlin_phantom4missions

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import dji.common.mission.activetrack.ActiveTrackTargetState
import dji.common.mission.activetrack.SubjectSensingState

class MultiTrackingView(
    context: Context
) : RelativeLayout(context) {

    private var mValueIndex: TextView? = null
    private var mRectF: ImageView? = null

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.layout_multi_tracking, null)
        this.addView(view)
        mValueIndex = findViewById<View>(R.id.index_textview) as TextView
        mRectF = findViewById<View>(R.id.tracking_rectf_iv) as ImageView
    }


    fun updateView(information: SubjectSensingState) {
        val targetState = information.state
        if (targetState == ActiveTrackTargetState.CANNOT_CONFIRM
            || targetState == ActiveTrackTargetState.UNKNOWN
        ) {
            mRectF?.setImageResource(R.drawable.visual_track_cannotconfirm)
        } else if (targetState == ActiveTrackTargetState.WAITING_FOR_CONFIRMATION) {
            mRectF?.setImageResource(R.drawable.visual_track_needconfirm)
        } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_LOW_CONFIDENCE) {
            mRectF?.setImageResource(R.drawable.visual_track_lowconfidence)
        } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_HIGH_CONFIDENCE) {
            mRectF?.setImageResource(R.drawable.visual_track_highconfidence)
        }
        mValueIndex?.text = "" + information.index
    }

}