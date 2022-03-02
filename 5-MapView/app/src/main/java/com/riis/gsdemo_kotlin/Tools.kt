package com.riis.gsdemo_kotlin

import android.app.Activity
import android.widget.Toast

object Tools {
    fun getInt(str: String?, defaultValue: Int): Int { // converts string to int
        return try {
            Integer.valueOf(str)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun getFloat(str: String?, defaultValue: Float): Float { // converts float to int
        return try {
            java.lang.Float.valueOf(str)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun showToast(activity: Activity, msg: String?) {
        activity.runOnUiThread { Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show() }
    }
}