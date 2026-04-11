package com.klipy.klipy_demo

import android.app.Application
import com.klipy.klipy_ui.KlipyUi
import com.klipy.sdk.KlipySdk

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        KlipyUi.configure {
            KlipySdk.create(
                context = this,
                secretKey = BuildConfig.KLIPY_API_KEY,
                enableLogging = true
            )
        }
    }
}
