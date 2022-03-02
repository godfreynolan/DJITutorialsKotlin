package com.riis.kotlin_mediamanagerdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class DefaultLayoutActivity : AppCompatActivity() {

    private lateinit var mediaManagerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_layout)

        mediaManagerBtn = findViewById(R.id.btn_mediaManager)
        mediaManagerBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}