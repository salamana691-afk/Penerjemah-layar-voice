package com.autodub.edge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.autodub.edge.domain.model.CaptureStatus
import com.autodub.edge.service.OverlayService
import com.autodub.edge.service.ScreenCaptureService
import com.autodub.edge.ui.navigation.AutoDubAppScaffold
import com.autodub.edge.ui.theme.AutoDubEdgeTheme
import com.autodub.edge.viewmodel.AutoDubViewModelFactory
import com.autodub.edge.viewmodel.CharacterViewModel
import com.autodub.edge.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { (application as AutoDubApplication).container }
    private val viewModelFactory by lazy { AutoDubViewModelFactory(appContainer) }

    private val mainViewModel: MainViewModel by viewModels { viewModelFactory }
    private val characterViewModel: CharacterViewModel by viewModels { viewModelFactory }

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(true)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissions()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            ContextCompat.startForegroundService(
                this,
                OverlayService.createStartIntent(this)
            )
            ContextCompat.startForegroundService(
                this,
                ScreenCaptureService.createStartIntent(this, result.resultCode, data)
            )
        } else {
            appContainer.pipelineCoordinator.updateCaptureStatus(
                CaptureStatus.IDLE,
                "Izin screen capture ditolak oleh pengguna."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissions()

        setContent {
            AutoDubEdgeTheme {
                AutoDubAppScaffold(
                    mainViewModel = mainViewModel,
                    characterViewModel = characterViewModel,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestNotificationPermission = { requestNotificationPermission() },
                    onStartCaptureFlow = { startCaptureAndOverlay() },
                    onStopCaptureAndOverlay = { stopAllServices() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startCaptureAndOverlay() {
        refreshPermissions()
        if (!hasOverlayPermission) {
            requestOverlayPermission()
            return
        }
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        if (projectionManager == null) {
            appContainer.pipelineCoordinator.updateCaptureStatus(
                CaptureStatus.ERROR,
                "MediaProjectionManager tidak tersedia di perangkat ini."
            )
            return
        }

        appContainer.pipelineCoordinator.updateCaptureStatus(CaptureStatus.REQUESTING_PERMISSION)
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun stopAllServices() {
        stopService(ScreenCaptureService.createStopIntent(this))
        stopService(OverlayService.createStopIntent(this))
        mainViewModel.resetSession()
    }
}
