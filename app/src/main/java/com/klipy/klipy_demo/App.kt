package com.klipy.klipy_demo

import android.app.Application
import com.klipy.klipy_ui.KlipyUi
import com.klipy.sdk.KlipySdk

class App : Application() {
    override fun onCreate() {
        super.onCreate()
//        val repo = KlipySdk.create(
//            context = this,
//            secretKey = "gBAJiSCmnYiDLeGoUSBddk8FwuWFaLDMJ24vUBlalQS4IkCoBpznFZPZpBj1QZfh",
//            enableLogging = true
//        )
//
//        KlipyUi.configure(repo)
    }
}