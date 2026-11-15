package com.grad.pleosvillage

import android.content.Intent
import android.os.Bundle
import com.grad.pleosvillage.vehicle.NaviDataSource
import com.grad.pleosvillage.data.AppRepository
import com.grad.pleosvillage.data.DecorStore
import com.grad.pleosvillage.ui.debug.DemoCaption
import com.grad.pleosvillage.ui.debug.DemoCaptionOverlay
import com.grad.pleosvillage.ui.onboarding.OnboardingPrefs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grad.pleosvillage.service.RecordingService
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.LocalDrivingRestricted
import com.grad.pleosvillage.ui.diagnostics.DiagnosticsScreen
import com.grad.pleosvillage.ui.nav.AppNav
import com.grad.pleosvillage.ui.theme.PleosVillageTheme
import com.grad.pleosvillage.vehicle.Reading
import com.grad.pleosvillage.vehicle.VehicleState

class MainActivity : ComponentActivity() {

    /**
     * Debug screen navigation (debug builds only):
     *   adb shell am start -n com.grad.pleosvillage/.MainActivity --es route dex
     *   --es route arrival/below   --ez narrow true (forces the narrow layout at 571dp)
     */
    private val requestedRoute = mutableStateOf<String?>(null)
    private val narrow = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // RecordingService receives vehicle data. The UI only displays those values.
        AppRepository.init(this)
        RecordingService.start(this)
        // Route guidance for the idle screen (read-only). Shows nothing if there is no navigation.
        NaviDataSource.start(this)
        // Only on first creation: a configuration change (e.g. night mode) recreates the activity with the
        // original launch intent, and its debug commands (such as "reset") must not run again.
        if (savedInstanceState == null) readIntent(intent)
        setContent {
            val vehicle by RecordingService.vehicleState.collectAsState(initial = VehicleState())
            val restricted = (vehicle.drivingRestricted as? Reading.Value)?.value == true
            // Night = system night mode (test with adb shell cmd uimode night yes). REFERENCES §2.
            PleosVillageTheme {
                CompositionLocalProvider(LocalDrivingRestricted provides restricted) {
                    val forced = if (narrow.value) 571.dp else null
                    DesignScaled(if (forced != null) Modifier.width(forced) else Modifier.fillMaxSize(), forceWidth = forced) {
                        AppNav(
                            diagnostics = { DiagnosticsScreen(vehicle, Modifier.fillMaxSize()) },
                            requestedRoute = requestedRoute.value,
                            onRouteConsumed = { requestedRoute.value = null },
                        )
                        // Demo video caption (debug builds only; empty and invisible by default)
                        if (BuildConfig.DEBUG) DemoCaptionOverlay()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) NaviDataSource.stop()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        intent.getStringExtra("route")?.let { requestedRoute.value = it }
        if (intent.hasExtra("narrow")) narrow.value = intent.getBooleanExtra("narrow", false)
        // Demo setup automation (debug builds only, same as the debug menu buttons):
        //   --es demo "reset,scale60,filteroff,demo"
        intent.getStringExtra("demo")?.split(',')?.forEach { cmd ->
            when (cmd.trim()) {
                "reset" -> AppRepository.resetAll()
                "today" -> AppRepository.resetToday()
                "scale60" -> AppRepository.setTimeScale(60)
                "scale1" -> AppRepository.setTimeScale(1)
                "filteroff" -> AppRepository.setSpeedFilter(false)
                "filteron" -> AppRepository.setSpeedFilter(true)
                "demo" -> AppRepository.loadDemo()
                "endtrip" -> AppRepository.endTrip()
                // Demo video: back to first-launch state (onboarding again, initial decorate layout)
                "onboarding" -> { OnboardingPrefs.setDone(this, false); requestedRoute.value = "onboarding" }
                "decorreset" -> DecorStore.reset()
                "naviOn" -> NaviDataSource.setDemo(true)
                "naviOff" -> NaviDataSource.setDemo(false)
            }
        }
        // Demo video caption: --es caption "글자" (an empty string hides it)
        intent.getStringExtra("caption")?.let { DemoCaption.text = it }
    }
}
