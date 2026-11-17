package com.autodub.edge.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.autodub.edge.AutoDubApplication
import com.autodub.edge.MainActivity
import com.autodub.edge.R
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.RoiMode
import com.autodub.edge.overlay.FloatingControlBar
import com.autodub.edge.overlay.RoiOverlay
import com.autodub.edge.overlay.TranslationOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class OverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private var translationView: ComposeView? = null
    private var translationParams: WindowManager.LayoutParams? = null

    private var controlBarView: ComposeView? = null
    private var controlBarParams: WindowManager.LayoutParams? = null

    private var roiView: ComposeView? = null
    private var roiParams: WindowManager.LayoutParams? = null

    private val container by lazy { (application as AutoDubApplication).container }
    private val coordinator by lazy { container.pipelineCoordinator }
    private val preferencesRepository by lazy { container.preferencesRepository }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            coordinator.updateOverlayRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundCompat()

        if (lifecycleOwner == null) {
            val owner = OverlayLifecycleOwner().also { it.onCreate() }
            lifecycleOwner = owner
            attachOverlays(owner)
            observeStateChanges()
            coordinator.updateOverlayRunning(true)
        }

        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                AppConstants.NOTIFICATION_ID_OVERLAY,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(AppConstants.NOTIFICATION_ID_OVERLAY, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            20,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            21,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_OVERLAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("AutoDub Edge Overlay")
            .setContentText("Overlay terjemahan & kontrol aktif di atas game")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notification_stop_action), stopIntent)
            .build()
    }

    private fun attachOverlays(owner: OverlayLifecycleOwner) {
        val wm = windowManager ?: return
        val metrics = resources.displayMetrics

        // 1. Translation Floating Overlay
        val tParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (metrics.widthPixels * 0.07f).roundToInt()
            y = (metrics.heightPixels * 0.10f).roundToInt()
        }
        translationParams = tParams

        val tView = createOwnedComposeView(owner).apply {
            setContent {
                val runtime by coordinator.runtimeState.collectAsState()
                val overlayConfig by coordinator.overlayConfigState.collectAsState()

                if (overlayConfig.isTranslationVisible) {
                    TranslationOverlay(
                        speakerName = runtime.currentSpeaker,
                        sourceText = runtime.currentSourceText,
                        translatedText = runtime.currentTranslatedText,
                        errorMessage = runtime.lastErrorMessage,
                        config = overlayConfig,
                        onDrag = { dx, dy ->
                            tParams.x += dx.roundToInt()
                            tParams.y += dy.roundToInt()
                            runCatching { wm.updateViewLayout(this, tParams) }
                        }
                    )
                }
            }
        }
        translationView = tView
        wm.addView(tView, tParams)

        // 2. Floating Control Bar
        val cParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 110
        }
        controlBarParams = cParams

        val cView = createOwnedComposeView(owner).apply {
            setContent {
                val runtime by coordinator.runtimeState.collectAsState()
                FloatingControlBar(
                    isPaused = runtime.isTranslationPaused,
                    isTranslationVisible = runtime.isTranslationVisible,
                    isVoiceEnabled = runtime.isVoiceEnabled,
                    roiMode = runtime.roiMode,
                    onTogglePause = { coordinator.togglePause() },
                    onToggleTranslationVisible = { coordinator.toggleTranslationVisibility() },
                    onToggleVoice = { coordinator.toggleVoiceEnabled() },
                    onToggleRoiSetup = {
                        val nextMode = if (runtime.roiMode == RoiMode.SETUP) RoiMode.SCAN else RoiMode.SETUP
                        coordinator.setRoiMode(nextMode)
                    },
                    onOpenSettings = {
                        val launchIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(launchIntent)
                    },
                    onStopAll = {
                        stopService(ScreenCaptureService.createStopIntent(this@OverlayService))
                        coordinator.resetSessionCaches()
                        stopSelf()
                    },
                    onDrag = { dx, dy ->
                        cParams.x += dx.roundToInt()
                        cParams.y += dy.roundToInt()
                        runCatching { wm.updateViewLayout(this, cParams) }
                    }
                )
            }
        }
        controlBarView = cView
        wm.addView(cView, cParams)
    }

    private fun observeStateChanges() {
        serviceScope.launch {
            coordinator.roiLayoutState.collectLatest { roiConfig ->
                if (roiConfig.mode == RoiMode.SETUP) {
                    showRoiSetupOverlay()
                } else {
                    hideRoiSetupOverlay()
                }
            }
        }
    }

    private fun showRoiSetupOverlay() {
        val wm = windowManager ?: return
        val owner = lifecycleOwner ?: return
        if (roiView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        roiParams = params

        val view = createOwnedComposeView(owner).apply {
            setContent {
                val layout by coordinator.roiLayoutState.collectAsState()
                RoiOverlay(
                    initialLayout = layout,
                    onSaveAndScan = { updated ->
                        serviceScope.launch {
                            preferencesRepository.saveRoiLayout(updated.copy(mode = RoiMode.SCAN))
                        }
                    },
                    onResetDefault = {
                        serviceScope.launch {
                            preferencesRepository.resetDefaultRoiLayout()
                        }
                    }
                )
            }
        }
        roiView = view
        runCatching { wm.addView(view, params) }
    }

    private fun hideRoiSetupOverlay() {
        val wm = windowManager ?: return
        roiView?.let { view ->
            runCatching { wm.removeView(view) }
            view.disposeComposition()
        }
        roiView = null
        roiParams = null
    }

    private fun createOwnedComposeView(owner: OverlayLifecycleOwner): ComposeView {
        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
        }
    }

    override fun onDestroy() {
        val wm = windowManager
        hideRoiSetupOverlay()
        translationView?.let { view ->
            runCatching { wm?.removeView(view) }
            view.disposeComposition()
        }
        translationView = null

        controlBarView?.let { view ->
            runCatching { wm?.removeView(view) }
            view.disposeComposition()
        }
        controlBarView = null

        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        serviceScope.cancel()
        coordinator.updateOverlayRunning(false)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.autodub.edge.action.START_OVERLAY"
        const val ACTION_STOP = "com.autodub.edge.action.STOP_OVERLAY"

        fun createStartIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java).apply { action = ACTION_START }

        fun createStopIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java).apply { action = ACTION_STOP }
    }
}
