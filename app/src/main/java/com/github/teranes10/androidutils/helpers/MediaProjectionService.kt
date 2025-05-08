package com.github.teranes10.androidutils.helpers

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.github.teranes10.androidutils.extensions.IntentExtensions.parcelable
import com.github.teranes10.androidutils.utils.PermissionUtil
import java.io.File

abstract class MediaProjectionService : ForegroundService() {
    protected val handler = Handler(Looper.getMainLooper())
    protected var mediaProjectionManager: MediaProjectionManager? = null; private set
    protected var mediaProjection: MediaProjection? = null; private set
    protected var mediaRecorder: MediaRecorder? = null; private set
    protected var virtualDisplay: VirtualDisplay? = null; private set
    protected var intent: Intent? = null; private set
    protected var resultCode: Int? = null; private set
    protected abstract fun onStartService(context: Context, resultCode: Int, intent: Intent)

    @RequiresApi(Build.VERSION_CODES.Q)
    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    override val startType = START_NOT_STICKY

    override fun onCommand(intent: Intent, flags: Int, startId: Int): Int? {
        if (intent.action == ACTION_START_FOREGROUND_SERVICE) {
            this.mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            this.resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED)
            this.intent = intent.parcelable<Intent>("data")

            if (this.mediaProjectionManager == null || this.resultCode != RESULT_OK || this.intent == null) {
                return START_NOT_STICKY
            }
        }

        return null
    }

    override fun onStartService(context: Context) {
        onStartService(context, resultCode!!, intent!!)
    }

    override fun onStopService(context: Context) {
        stopRecording()
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION") (MediaRecorder())
        }
    }

    fun startRecording(file: File, width: Int = 1280, height: Int = 720, bitrate: Int = 5_000_000, frameRate: Int = 30) {
        if (resultCode != RESULT_OK || intent == null) {
            Log.e(TAG, "startRecording: result code or intent is not valid")
            return
        }

        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode!!, intent!!)
        if (mediaProjection == null) {
            Log.e(TAG, "startRecording: media projection is null")
            return
        }

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopRecording()
            }
        }, handler)

        mediaRecorder = createMediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            if (PermissionUtil.hasAudioPermission(context)) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file.absolutePath)

            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoEncodingBitRate(bitrate)
            setVideoFrameRate(frameRate)

            if (PermissionUtil.hasAudioPermission(context)) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }

            prepare()
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            TAG,
            width,
            height,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )

        mediaRecorder?.start()
    }

    fun stopRecording() {
        virtualDisplay?.release()
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        mediaProjection?.stop()

        virtualDisplay = null
        mediaRecorder = null
        mediaProjection = null
    }

    companion object {
        const val TAG = "MediaProjectionService"
    }
}

class MediaProjectionServiceHelper(private val context: FragmentActivity, service: Class<out MediaProjectionService>) {
    private lateinit var pendingExtras: Bundle

    private var getScreenCapture: ActivityResultLauncher<Intent> = context.activityResultRegistry.register(
        MediaProjectionService.TAG, context, ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result != null && result.resultCode == RESULT_OK) {
            val extras = Bundle(pendingExtras).apply {
                putInt("resultCode", result.resultCode)
                putParcelable("data", result.data)
            }
            ForegroundService.startService(context, service, extras)
        } else {
            Toast.makeText(context, "Screen capture permission denied", Toast.LENGTH_LONG).show()
        }
    }

    fun startService(extras: Bundle = Bundle()) {
        pendingExtras = extras
        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        getScreenCapture.launch(captureIntent)
    }
}