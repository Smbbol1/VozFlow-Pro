package com.wlqboard.nativeapp

import android.app.Application

class VozFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
