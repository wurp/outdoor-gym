package com.vibedev.app

import android.app.Application

class VibeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Test builds save crash details so the author can paste them to Claude.
        if (BuildConfig.DEBUG) CrashCatcher.install(this)
    }
}
