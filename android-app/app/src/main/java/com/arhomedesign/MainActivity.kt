package com.arhomedesign

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arhomedesign.core.crash.CrashReporting
import com.arhomedesign.core.data.AppPreferences
import com.arhomedesign.homedesign.navigation.HomeDesignApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var loading = true
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { loading }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        val crashLog = CrashReporting.readLastCrash(this)
        lifecycleScope.launch {
            val onboardingDone = AppPreferences(this@MainActivity).isOnboardingComplete()
            loading = false
            setContent {
                HomeDesignApp(
                    showOnboarding = !onboardingDone,
                    crashLog = crashLog,
                    onCrashShared = { CrashReporting.clearLastCrash(this@MainActivity) },
                )
            }
        }
    }
}
