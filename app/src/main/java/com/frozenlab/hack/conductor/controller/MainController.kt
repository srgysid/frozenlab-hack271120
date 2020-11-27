package com.frozenlab.hack.conductor.controller

import android.Manifest
import android.content.ContentValues
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.frozenlab.hack.Preferences
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.databinding.ControllerMainBinding
import com.frozenlab.hack.websocket.VoiceWebSocketListener
import com.frozenlab.hack.websocket.getUnsafeOkHttpClientBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit

class MainController: BaseController {

    companion object {
        private const val WEB_SOCKET_CLOSE_NORMAL_CODE   = 1000
        private const val WEB_SOCKET_CLOSE_NORMAL_REASON = "finished"
        private const val WEB_SOCKET_BUFFER_SIZE         = 8192
    }

    constructor(): super()
    constructor(args: Bundle): super(args)

    //private var recordStream:   FileOutputStream? = null
    private var webSocket:   WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var isReading:   Boolean = false
    //private var _mediaRecorder: MediaRecorder? = null
    // private val mediaRecorder:  MediaRecorder get() = _mediaRecorder!!


    override val binding: ControllerMainBinding get() = _binding!! as ControllerMainBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerMainBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

        getPermissions() {
            binding.buttonVoice.setOnTouchListener(voiceTouchListener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord?.release()
        webSocket?.cancel()
    }

    private var voiceTouchListener = object: View.OnTouchListener {

        private var mHandler: Handler? = null
        private var start: Long = 0

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {

            when (event?.action) {

                MotionEvent.ACTION_DOWN -> {

                    start = Calendar.getInstance().timeInMillis

                    if (mHandler != null) return true
                    mHandler = Handler()
                    mHandler!!.postDelayed(mAction, 500)
                }

                MotionEvent.ACTION_UP -> {

                    if (mHandler == null) return true
                    mHandler!!.removeCallbacks(mAction)
                    mHandler = null

                    if (Calendar.getInstance().timeInMillis - start < 500) {
                        view?.performClick()
                    } else {
                        recordStop()
                    }
                }
            }

            return false
        }

        var mAction: Runnable = object: Runnable {
            override fun run() {
                if(!isReading) {
                    recordStart()
                }
                mHandler?.postDelayed(this, 500);
            }
        }
    }
    /*
        private fun mediaRecorderCreate() {
            if(_mediaRecorder != null) {
                _mediaRecorder?.release()
                _mediaRecorder = null
            }

            _mediaRecorder = MediaRecorder()
        }

        private fun mediaRecorderIsReady(): Boolean {
            return _mediaRecorder != null
        }

        private fun mediaRecorderDestroy() {
            _mediaRecorder?.run {
                _mediaRecorder?.release()
                _mediaRecorder = null
            }
        }
    */
    private fun createAudioRecord() {

        val sampleRate    = 8000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat   = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize    = 4 * AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
    }

    private fun createWebSocket() {

        val timeout = Preferences.okHttpSocketTimeOut

        val okHttpClient = getUnsafeOkHttpClientBuilder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(Preferences.voiceUrl)
            .build()

        val listener = VoiceWebSocketListener()

        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    private fun streamToWebSocket() {
        audioRecord?.startRecording()
        isReading = true
        Thread {
            if(audioRecord == null) return@Thread
            val buffer = ByteArray(WEB_SOCKET_BUFFER_SIZE)
            var count: Int
            while (isReading) {
                count = audioRecord?.read(buffer, 0, WEB_SOCKET_BUFFER_SIZE) ?: -1
                Timber.d("readed: ${count}")
                if(webSocket?.send(buffer.toByteString()) != true) {
                    Timber.e("Web socket error")
                }
            }
        }.start()
    }

    private fun recordStart() {
        try {

            if(audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                createAudioRecord()
            }

            if(webSocket == null) {
                createWebSocket()
            }

            streamToWebSocket()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun recordStop() {

        isReading = false

        if(audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                audioRecord?.stop()
            } catch (e: Exception) {
                Timber.e(e.localizedMessage)
            }
        }

        try {
            webSocket?.close(WEB_SOCKET_CLOSE_NORMAL_CODE, WEB_SOCKET_CLOSE_NORMAL_REASON)
            webSocket = null
        } catch (e: Exception) {
            Timber.e(e.localizedMessage)
        }

    }

    private fun getPermissions(onSuccess: () -> Unit) {

        val permList = arrayListOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

        val needPermissions = mainActivity.requestPermissionsIfNeed(permList) {
            getPermissions(onSuccess)
        }

        if(needPermissions) {
            return
        }

        onSuccess.invoke()
    }

    private fun getFileOutputStream(): FileOutputStream? {

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "record.wav")
            put(MediaStore.Images.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        }

        val contentResolver = mainActivity.contentResolver

        return contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )?.let { outputUri ->
            try {
                contentResolver.openFileDescriptor(outputUri, "w")?.let {
                    FileOutputStream(it.fileDescriptor)
                }
            } catch (exception: FileNotFoundException) {
                return null
            } catch (exception: Exception) {
                return null
            }
        }
    }

    private fun openVoiceSocket(): WebSocket {

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(Preferences.voiceUrl)
            .build()

        val listener = VoiceWebSocketListener()

        return client.newWebSocket(request, listener)
    }
}
