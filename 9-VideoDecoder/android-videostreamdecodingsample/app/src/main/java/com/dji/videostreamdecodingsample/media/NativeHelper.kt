package com.dji.videostreamdecodingsample.media

import com.dji.videostreamdecodingsample.media.NativeHelper.NativeDataListener
import com.dji.videostreamdecodingsample.media.NativeHelper

/**
 * A helper class to invoke native methods
 */
object NativeHelper {
    interface NativeDataListener {
        /**
         * Callback method for receiving the frame data from NativeHelper.
         * Note that this method will be invoke in framing thread, which means time consuming
         * processing should not in this thread, or the framing process will be blocked.
         * @param data
         * @param size
         * @param frameNum
         * @param isKeyFrame
         * @param width
         * @param height
         */
        fun onDataRecv(
            data: ByteArray?,
            size: Int,
            frameNum: Int,
            isKeyFrame: Boolean,
            width: Int,
            height: Int
        )
    }

    private var dataListener: NativeDataListener? = null
    fun setDataListener(dataListener: NativeDataListener?) {
        this.dataListener = dataListener
    }
    //JNI
    /**
     * Test the ffmpeg.
     * @return
     */
    external fun codecinfotest(): String?

    /**
     * Initialize the ffmpeg.
     * @return
     */
    external fun init(): Boolean

    /**
     * Framing the raw data from camera
     * @param buf
     * @param size
     * @return
     */
    external fun parse(buf: ByteArray?, size: Int): Boolean

    /**
     * Release the ffmpeg
     * @return
     */
    external fun release(): Boolean

    val instance: NativeHelper
    get() {
        return this
    }

    init {
        System.loadLibrary("ffmpeg")
        System.loadLibrary("djivideojni")
    }

    /**
     * Invoke by JNI
     * Callback the frame data.
     * @param buf
     * @param size
     * @param frameNum
     * @param isKeyFrame
     * @param width
     * @param height
     */
    fun onFrameDataRecv(
        buf: ByteArray?,
        size: Int,
        frameNum: Int,
        isKeyFrame: Boolean,
        width: Int,
        height: Int
    ) {
        if (dataListener != null) {
            dataListener!!.onDataRecv(buf, size, frameNum, isKeyFrame, width, height)
        }
    }
}