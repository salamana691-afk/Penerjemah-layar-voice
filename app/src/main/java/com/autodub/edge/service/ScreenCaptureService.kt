package com.autodub.edge.service

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.autodub.edge.AutoDubApplication
import com.autodub.edge.MainActivity
import com.autodub.edge.R
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.CaptureStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class ScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private var displayManager: DisplayManager? = null
    private var currentWidth: Int = 0
    private var currentHeight: Int = 0
    private var currentDensityDpi: Int = DisplayMetrics.DENSITY_DEFAULT

    private val lastProcessedFrameTimeMs = AtomicLong(0L)

    private val coordinator by lazy {
        (application as AutoDubApplication).container.pipelineCoordinator
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            coordinator.updateCaptureStatus(
                CaptureStatus.IDLE,
                "Tangkapan layar dihentikan oleh sistem."
            )
            stopSelf()
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            val metrics = readDisplayMetrics()
            if (metrics.widthPixels != currentWidth || metrics.heightPixels != currentHeight) {
                reconfigureVirtualDisplay(
                    metrics.widthPixels,
                    metrics.heightPixels,
                    metrics.densityDpi
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        displayManager = getSystemService(DisplayManager::class.java)
        displayManager?.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))

        captureThread = HandlerThread("AutoDubCaptureThread").apply { start() }
        captureHandler = Handler(captureThread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                coordinator.updateCaptureStatus(CaptureStatus.IDLE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != Activity.RESULT_OK || data == null) {
                    coordinator.updateCaptureStatus(
                        CaptureStatus.ERROR,
                        "Izin screen capture ditolak atau data proyeksi tidak valid."
                    )
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForegroundCompat()
                startProjection(resultCode, data)
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                AppConstants.NOTIFICATION_ID_CAPTURE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(AppConstants.NOTIFICATION_ID_CAPTURE, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, ScreenCaptureService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_CAPTURE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("AutoDub Edge Aktif")
            .setContentText("Memindai layar game untuk terjemahan & AI voice dubbing")
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.notification_stop_action), stopIntent)
            .build()
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        tearDownProjection()

        val projection = mediaProjectionManager?.getMediaProjection(resultCode, data)
        if (projection == null) {
            coordinator.updateCaptureStatus(
                CaptureStatus.ERROR,
                "MediaProjection tidak dapat dibuat."
            )
            stopSelf()
            return
        }

        mediaProjection = projection
        projection.registerCallback(projectionCallback, captureHandler)

        val metrics = readDisplayMetrics()
        reconfigureVirtualDisplay(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        coordinator.updateCaptureStatus(CaptureStatus.RUNNING)
    }

    private fun readDisplayMetrics(): DisplayMetrics {
        val wm = getSystemService(WindowManager::class.java)
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
            val bounds = wm.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width().coerceAtLeast(320)
            metrics.heightPixels = bounds.height().coerceAtLeast(320)
            metrics.densityDpi = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(metrics)
        }
        return metrics
    }

    private fun reconfigureVirtualDisplay(width: Int, height: Int, densityDpi: Int) {
        val projection = mediaProjection ?: return

        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null

        currentWidth = width
        currentHeight = height
        currentDensityDpi = densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader.setOnImageAvailableListener({ r ->
            onFrameAvailable(r)
        }, captureHandler)
        imageReader = reader

        virtualDisplay = projection.createVirtualDisplay(
            "AutoDubVirtualDisplay",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            captureHandler
        )
    }

    private fun onFrameAvailable(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: return

            val now = System.currentTimeMillis()
            val intervalMs = coordinator.dubbingSettingsState.value.ocrIntervalMs
            if (now - lastProcessedFrameTimeMs.get() < intervalMs) {
                return
            }
            lastProcessedFrameTimeMs.set(now)

            val bitmap = imageToBitmap(image) ?: return
            serviceScope.launch {
                try {
                    coordinator.processScreenBitmap(bitmap)
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
        } catch (_: Throwable) {
            // Protect capture loop from transient surface resize frames
        } finally {
            image?.close()
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        if (pixelStride <= 0) return null

        val paddedWidth = image.width + rowPadding / pixelStride
        val rawBitmap = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        rawBitmap.copyPixelsFromBuffer(buffer)

        return if (paddedWidth == image.width) {
            rawBitmap
        } else {
            val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, image.width, image.height)
            rawBitmap.recycle()
            cropped
        }
    }

    private fun tearDownProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        displayManager?.unregisterDisplayListener(displayListener)
        tearDownProjection()
        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null
        serviceScope.cancel()
        coordinator.updateCaptureStatus(CaptureStatus.IDLE)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.autodub.edge.action.START_CAPTURE"
        const val ACTION_STOP = "com.autodub.edge.action.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        fun createStartIntent(context: Context, resultCode: Int, data: Intent): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
