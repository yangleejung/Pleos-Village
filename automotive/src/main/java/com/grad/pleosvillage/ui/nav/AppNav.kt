package com.grad.pleosvillage.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.grad.pleosvillage.ui.design.LocalDrivingRestricted
import com.grad.pleosvillage.vehicle.NaviDataSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grad.pleosvillage.ui.village.IDLE_TIMEOUT_MS
import com.grad.pleosvillage.ui.village.IdleController
import com.grad.pleosvillage.ui.village.IdleVillage
import com.grad.pleosvillage.ui.village.LocalIdleController
import com.grad.pleosvillage.ui.village.watchTouches
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.grad.pleosvillage.data.DecorStore
import com.grad.pleosvillage.ui.decorate.DecorateScreen
import androidx.compose.ui.platform.LocalContext
import com.grad.pleosvillage.BuildConfig
import com.grad.pleosvillage.ui.debug.DebugMenuScreen
import com.grad.pleosvillage.ui.onboarding.OnboardingPrefs
import com.grad.pleosvillage.ui.onboarding.OnboardingScreen
import com.grad.pleosvillage.ui.records.RecordsScreen
import com.grad.pleosvillage.ui.settings.SettingsScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.data.AppRepository
import com.grad.pleosvillage.data.RealUi
import com.grad.pleosvillage.ui.arrival.ArrivalScreen
import com.grad.pleosvillage.ui.art.MockVillageSpec
import com.grad.pleosvillage.ui.art.VillageCanvas
import com.grad.pleosvillage.ui.debug.GalleryScreen
import com.grad.pleosvillage.ui.dex.DexScreen
import com.grad.pleosvillage.ui.village.VillageScreen

/*
 * Screen routes. UI_SPEC §6: village, dex, records, settings, arrival/{summaryId}, trip/{tripId}, decorate, onboarding, debug.
 * The village is always at the bottom (start destination); on first launch, onboarding is shown on top of it.
 */

private const val TRANSITION_MS = 250
private const val ONBOARDING = "onboarding"
private const val DEBUG = "debug"
private const val DECORATE = "decorate"

@Composable
fun AppNav(
    diagnostics: @Composable () -> Unit,
    requestedRoute: String?,
    onRouteConsumed: () -> Unit,
    nav: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // On first launch, show onboarding (UI_SPEC §2). The village stays the start destination and onboarding goes on top.
        if (!OnboardingPrefs.done(context)) nav.navigate(ONBOARDING) { launchSingleTop = true }
    }
    LaunchedEffect(requestedRoute) {
        requestedRoute?.let { nav.navigate(it) { launchSingleTop = true }; onRouteConsumed() }
    }
    val selectTab: (Tab) -> Unit = { tab ->
        if (tab == Tab.Village) {
            // The village is the start destination. Navigating with saveState/restoreState would restore the screen
            // just popped (dex etc.) as the village's saved state, landing back where we were. So pop everything above it instead.
            if (!nav.popBackStack(Tab.Village.route, inclusive = false)) {
                nav.navigate(Tab.Village.route) { launchSingleTop = true }
            }
        } else {
            nav.navigate(tab.route) {
                popUpTo(Tab.Village.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    // Real data (judging engine). When a stop produces a new arrival summary, show it right away if the screen is visible (SPEC 6.3).
    val engine by AppRepository.state.collectAsState()
    val decorSaved by DecorStore.saved.collectAsState()
    val drivingNow = LocalDrivingRestricted.current
    LaunchedEffect(engine.unseen, drivingNow) {
        val id = engine.unseen ?: return@LaunchedEffect
        if (drivingNow) return@LaunchedEffect
        nav.navigate("arrival/$id") { launchSingleTop = true }
    }
    // Idle mode: after 10 seconds without touch on a main tab, only the village art remains.
    val idle = remember { IdleController() }
    var idling by remember { mutableStateOf(false) }
    // While driving (driving restriction), the idle screen is shown over any screen and cannot be woken.
    val driving = LocalDrivingRestricted.current
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val idleAllowed = route in Tab.entries.map { it.route } && idle.blockers == 0
    LaunchedEffect(idle.lastTouch, idleAllowed, idling) {
        if (!idleAllowed || idling) return@LaunchedEffect
        delay(IDLE_TIMEOUT_MS)
        idling = true
    }
    if (!idleAllowed && idling && !driving) idling = false
    CompositionLocalProvider(LocalIdleController provides idle) {
    Box(Modifier.fillMaxSize().watchTouches(idle)) {
        NavHost(
            nav, startDestination = Tab.Village.route,
            enterTransition = { fadeIn(tween(TRANSITION_MS)) },
            exitTransition = { fadeOut(tween(TRANSITION_MS)) },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Tab.Village.route) {
                VillageScreen(
                RealUi.village(engine, decorSaved), selectTab,
                onDecorate = { nav.navigate(DECORATE) { launchSingleTop = true } },
            )
            }
            // Decorate (UI_SPEC 4.3). Returns to the village after [완료] or back ("저장할까요?").
            composable(DECORATE) { DecorateScreen(onDone = { nav.popBackStack() }) }
            composable(Tab.Dex.route) { DexScreen(RealUi.dex(engine), selectTab, onShowInVillage = { selectTab(Tab.Village) }) }
            composable(Tab.Records.route) {
                RecordsScreen(RealUi.records(engine), selectTab, onOpenTrip = { id -> nav.navigate("trip/$id") })
            }
            composable(Tab.Settings.route) {
                SettingsScreen(
                    FakeRepository.settings, RealUi.dex(engine), selectTab,
                    onOnboarding = { nav.navigate(ONBOARDING) { launchSingleTop = true } },
                    onOpenDebug = { if (BuildConfig.DEBUG) nav.navigate(DEBUG) { launchSingleTop = true } },
                )
            }
            composable("arrival/{id}") { entry ->
                val id = entry.arguments?.getString("id") ?: "met"
                // Real stop summary (numeric id) first; otherwise a debug sample state (met, below, ...)
                val ui = RealUi.arrivalById(engine, id) ?: FakeRepository.arrivals[id] ?: FakeRepository.arrivals.getValue("met")
                LaunchedEffect(id) { id.toLongOrNull()?.let { AppRepository.markSeen(it) } }
                ArrivalScreen(
                    ui,
                    onClose = { if (!nav.popBackStack()) selectTab(Tab.Village) },
                    onGoVillage = { selectTab(Tab.Village) },
                )
            }
            // Trip detail: shows that trip's arrival summary again without animation (UI_SPEC 4.6).
            composable("trip/{id}") { entry ->
                val tid = entry.arguments?.getString("id") ?: ""
                val ui = RealUi.arrivalById(engine, tid) ?: FakeRepository.tripSummary(tid)
                if (ui == null) LaunchedEffect(Unit) { nav.popBackStack() }
                else ArrivalScreen(ui, onClose = { nav.popBackStack() }, onGoVillage = null, animate = false)
            }
            composable(ONBOARDING) {
                OnboardingScreen(onFinish = {
                    OnboardingPrefs.setDone(context, true)
                    if (!nav.popBackStack()) selectTab(Tab.Village)
                })
            }
            composable(DEBUG) {
                DebugMenuScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { route -> nav.navigate(route) },
                    onResetOnboarding = { OnboardingPrefs.setDone(context, false); nav.navigate(ONBOARDING) },
                onOpenDex = { selectTab(Tab.Dex) },
                )
            }
            composable("debug/diag") { diagnostics() }
            composable("debug/gallery") { GalleryScreen() }
            composable("debug/art") { VillageCanvas(MockVillageSpec, Modifier.fillMaxSize()) }
        }
        AnimatedVisibility(idling || driving, enter = fadeIn(tween(TRANSITION_MS)), exit = fadeOut(tween(TRANSITION_MS))) {
            val navi by NaviDataSource.state.collectAsState()
            val naviDemo by NaviDataSource.demo.collectAsState()
            IdleVillage(RealUi.village(engine, decorSaved), onWake = { idling = false }, navi = navi ?: if (naviDemo) NaviDataSource.demoState() else null)
        }
    }
    }
}
