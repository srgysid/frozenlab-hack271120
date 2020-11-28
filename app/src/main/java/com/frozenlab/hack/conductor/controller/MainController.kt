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

    constructor(): super()
    constructor(args: Bundle): super(args)

    override val binding: ControllerMainBinding get() = _binding!! as ControllerMainBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerMainBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

    }
}
