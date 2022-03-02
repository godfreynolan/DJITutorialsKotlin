package com.riis.kotlin_simulatordemo

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import kotlin.math.*
import kotlin.properties.Delegates

class OnScreenJoystick(context: Context, attributeSet: AttributeSet): SurfaceView(
    context,
    attributeSet
), SurfaceHolder.Callback, View.OnTouchListener {

    private lateinit var mJoystick: Bitmap
    private lateinit var mHolder: SurfaceHolder
    private var mKnobBounds: Rect? = null
    private lateinit var mThread: JoystickThread

    private var mKnobX by Delegates.notNull<Int>()
    private var mKnobY by Delegates.notNull<Int>()
    private var mKnobSize by Delegates.notNull<Int>()
    private var mBackgroundSize by Delegates.notNull<Int>()
    private var mRadius by Delegates.notNull<Float>()

    private var mJoystickListener: OnScreenJoystickListener? = null
    private var mAutoCentering: Boolean = true

    init {
        initGraphics()
        init()
    }

    private fun init() {
        mHolder = holder
        mHolder.addCallback(this)

        mThread = JoystickThread()

        setZOrderOnTop(true)
        mHolder.setFormat(PixelFormat.TRANSPARENT)
        setOnTouchListener(this)
        isEnabled = true
        setAutoCentering(true)
    }

    private fun initGraphics() {
        val res = context.resources
        mJoystick = BitmapFactory.decodeResource(
            res, R.mipmap.joystick
        )
    }

    private fun initBounds(pCanvas: Canvas) {
        mBackgroundSize = pCanvas.height
        mKnobSize = (mBackgroundSize * 0.6f).roundToInt()
        mKnobBounds = Rect()
        mRadius = mBackgroundSize * 0.5f
        mKnobX = ((mBackgroundSize - mKnobSize) * 0.5f).roundToInt()
        mKnobY = ((mBackgroundSize - mKnobSize) * 0.5f).roundToInt()

    }

    fun setAutoCentering(autoCentering: Boolean) {mAutoCentering = autoCentering}

    fun isAutoCentering(): Boolean {
        return mAutoCentering
    }

    fun setJoystickListener(joystickListener: OnScreenJoystickListener) {
        mJoystickListener = joystickListener
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mThread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        mThread.running = false

        while (retry) {
            try {
                mThread.join()
                retry = false
            } catch (e: InterruptedException) {}
        }
    }

    private fun doDraw(canvas: Canvas) {
        if (mKnobBounds == null) {
            initBounds(canvas)
        }

        mKnobBounds?.let {
            it.set(mKnobX, mKnobY, mKnobX + mKnobSize, mKnobY + mKnobSize)
            canvas.drawBitmap(mJoystick, null, it, null)
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val x = event?.x ?: return false
        val y = event.y

        when(event.action) {
            MotionEvent.ACTION_UP -> {
                if (isAutoCentering()) {
                    mKnobX = ((mBackgroundSize - mKnobSize) * 0.5f).roundToInt()
                    mKnobY = ((mBackgroundSize - mKnobSize) * 0.5f).roundToInt()
                }
            }

            else -> {
                if (checkBounds(x, y)) {
                    mKnobX = (x - mKnobSize * 0.5f).roundToInt()
                    mKnobY = (y - mKnobSize * 0.5f).roundToInt()
                } else {
                    val angle: Float = atan2(y - mRadius, x - mRadius)
                    mKnobX = ((mRadius + (mRadius - mKnobSize * 0.5f) * cos(angle)).roundToInt() - mKnobSize * 0.5f).toInt()
                    mKnobY = ((mRadius + (mRadius - mKnobSize * 0.5f) * sin(angle)).roundToInt() - mKnobSize * 0.5f).toInt()
                }
            }
        }

        mJoystickListener?.onTouch(this,
            (0.5f - (mKnobX / (mRadius * 2 - mKnobSize))) * -2,
            (0.5f - (mKnobY / (mRadius * 2 - mKnobSize))) * 2
        )
        return true
    }

    private fun checkBounds(pX: Float, pY: Float): Boolean {
        return (mRadius - pX).pow(2) + (mRadius - pY).pow(2) <=
                (mRadius - mKnobSize * 0.5f).pow(2)
    }

    private inner class JoystickThread: Thread() {
        var running = false


        @Synchronized
        override fun start() {
            running = true
            super.start()
        }

        override fun run() {
            while(running) {
                var canvas: Canvas? = null
                try {
                    canvas = mHolder.lockCanvas(null)
                    synchronized(mHolder) {
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                        doDraw(canvas)
                    }
                } catch (e: Exception) {}

                finally {
                    if (canvas != null) {
                        mHolder.unlockCanvasAndPost(canvas)
                    }
                }

            }
        }
    }

}