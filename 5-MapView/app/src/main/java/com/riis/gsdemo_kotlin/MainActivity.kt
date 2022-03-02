package com.riis.gsdemo_kotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val clickListener =
        View.OnClickListener { v: View -> // based on what the user picks, it'll use either Waypoint 1 or 2
            when (v.id) {
                R.id.btn_waypoint1 -> startActivity(
                    this@MainActivity,
                    Waypoint1Activity::class.java
                )
                R.id.btn_waypoint2 -> startActivity(
                    this@MainActivity,
                    Waypoint2Activity::class.java
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_waypoint1).setOnClickListener(clickListener) // set the listener to the previously defined clickListener
        findViewById<View>(R.id.btn_waypoint2).setOnClickListener(clickListener)
    }

    private fun startActivity(context: Context, activity: Class<*>?) { // this will start the activity
        val intent = Intent(context, activity)
        context.startActivity(intent)
    }
}