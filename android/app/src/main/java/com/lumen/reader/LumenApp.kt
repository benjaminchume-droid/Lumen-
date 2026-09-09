package com.lumen.reader

import android.app.Application

class LumenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LumenApp
            private set
    }
}
