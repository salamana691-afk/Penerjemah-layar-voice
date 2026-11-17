package com.autodub.edge

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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

        val crashLog = intent.getStringExtra("CRASH_LOG")
        if (crashLog != null) {
            showCrashScreen(crashLog)
            return
        }

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val stackTrace = Log.getStackTraceString(throwable)
            val restartIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("CRASH_LOG", stackTrace)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(restartIntent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        try {
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
        } catch (t: Throwable) {
            showCrashScreen(Log.getStackTraceString(t))
        }
    }

    private fun showCrashScreen(errorText: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(0xFF0D1117.toInt())
        }
        val title = TextView(this).apply {
            text = "CRASH TERDETEKSI (Copy & kirim ke Gemini):"
            textSize = 16f
            setTextColor(0xFFFF5555.toInt())
        }
        val copyBtn = Button(this).apply {
            text = "SALIN LOG ERROR"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("CrashLog", errorText))
                Toast.makeText(this@MainActivity, "Error berhasil disalin!", Toast.LENGTH_SHORT).show()
            }
        }
        val scroll = ScrollView(this)
        val body = TextView(this).apply {
            text = errorText
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setTextIsSelectable(true)
        }
        scroll.addView(body)
        layout.addView(title)
        layout.addView(copyBtn)
        layout.addView(scroll)
        setContentView(layout)
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
