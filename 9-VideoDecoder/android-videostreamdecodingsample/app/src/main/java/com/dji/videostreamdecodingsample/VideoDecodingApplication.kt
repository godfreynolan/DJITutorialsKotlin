package com.dji.videostreamdecodingsample

import android.app.Application
import android.content.Context
import com.secneo.sdk.Helper
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKManager

class VideoDecodingApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        Helper.install(this@VideoDecodingApplication)
    }

    companion object {
        private var mProduct: BaseProduct? = null

        @get:Synchronized
        val productInstance: BaseProduct?
            get() {
                if (null == mProduct) {
                    mProduct = DJISDKManager.getInstance().product
                }
                return mProduct
            }

        @Synchronized
        fun updateProduct(product: BaseProduct?) {
            mProduct = product
        }
    }
}