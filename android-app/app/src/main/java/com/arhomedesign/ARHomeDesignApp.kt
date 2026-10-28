package com.arhomedesign

import android.app.Application
import com.arhomedesign.core.crash.CrashReporting
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ARHomeDesignApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        CrashReporting.init(this)
    }
}
