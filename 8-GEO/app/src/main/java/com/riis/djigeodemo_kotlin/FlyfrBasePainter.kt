package com.riis.djigeodemo_kotlin

import android.graphics.Color

class FlyfrBasePainter {

    var heightToColor: MutableMap<Int, Float> = HashMap()
    val colorTransparent = Color.argb(0, 0, 0, 0)

    init {
        /*heightToColor[65] = Color.argb(50, 0, 0, 0)
        heightToColor[125] = Color.argb(25, 0, 0, 0)*/
        heightToColor[65] = .2f
        heightToColor[125] = .1f
    }
}