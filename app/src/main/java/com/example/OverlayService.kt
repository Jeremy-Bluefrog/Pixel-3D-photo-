package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentUris
import android.provider.MediaStore
import com.example.ui.components.VideoOrMorphingLoader
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.draw.clip

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var overlayView: View? = null
    private var mediaProjection: MediaProjection? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("Pixel 3D AI 正在背景執行")
            .setContentText("提供螢幕上層截圖按鈕")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data: Intent? = intent?.getParcelableExtra("data")

        if (resultCode == -1 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data)

        showFloatingButton()

        return START_NOT_STICKY
    }

    private fun showFloatingButton() {
        if (floatingView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0
        params.y = 100

        floatingView = ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    IconButton(
                        onClick = { handleFloatingButtonClick() },
                        modifier = Modifier
                            .padding(16.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Pixel 3D AI",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        windowManager.addView(floatingView, params)
    }

    private fun handleFloatingButtonClick() {
        floatingView?.visibility = View.GONE
        showLoadingOverlay()

        scope.launch {
            // 1. Take Screenshot
            val bitmap = takeScreenshot()
            if (bitmap == null) {
                updateLoadingText("截圖失敗")
                delay(2000)
                hideLoadingOverlay()
                floatingView?.visibility = View.VISIBLE
                return@launch
            }

            // 2. Simulate or perform matching
            val matchUri = findMatchingPhotoInGallery()
            if (matchUri != null) {
                updateLoadingText("已找到相片，啟動中...")
                delay(1000)
                
                // 3. Launch MainActivity with Uri
                val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = matchUri
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                
                hideLoadingOverlay()
                stopSelf() // we are done
            } else {
                updateLoadingText("無法啟動")
                delay(2000)
                hideLoadingOverlay()
                floatingView?.visibility = View.VISIBLE
            }
        }
    }

    private var currentLoadingText by mutableStateOf("分析照片中...")

    private fun showLoadingOverlay() {
        if (overlayView != null) return
        currentLoadingText = "分析照片中..."

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        overlayView = ComposeView(this).apply {
            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        VideoOrMorphingLoader(size = 110.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = currentLoadingText,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun updateLoadingText(text: String) {
        currentLoadingText = text
    }

    private fun hideLoadingOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private suspend fun takeScreenshot(): Bitmap? = withContext(Dispatchers.IO) {
        val mp = mediaProjection ?: return@withContext null
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val virtualDisplay = mp.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        var bitmap: Bitmap? = null
        val lock = java.util.concurrent.CountDownLatch(1)

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bmp = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(buffer)
                
                bitmap = Bitmap.createBitmap(bmp, 0, 0, width, height)
                
                image.close()
                lock.countDown()
            }
        }, Handler(Looper.getMainLooper()))

        // Wait for max 1 second
        lock.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
        
        virtualDisplay?.release()
        imageReader.close()

        bitmap
    }

    private suspend fun findMatchingPhotoInGallery(): Uri? = withContext(Dispatchers.IO) {
        delay(1500) // Simulate processing time for matching
        // In a real app, we would compare the screenshot pixels with media store images.
        // For this prototype, we'll just return the most recent image from the gallery to simulate a successful match.
        
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = it.getLong(idIndex)
                return@withContext ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "overlay_channel",
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        overlayView?.let { windowManager.removeView(it) }
        mediaProjection?.stop()
    }
}
