package com.riis.kotlin_phantom4missions

import android.app.Application
import android.content.Context
import com.secneo.sdk.Helper

class MApplication : Application() {

    private var demoApplication: DJIDemoApplication? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Helper.install(this)

    }


}