package com.frozenlab.hack.conductor.controller

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.frozenlab.hack.Preferences
import com.frozenlab.hack.api.models.IssueDetails
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.conductor.controller.base.ToolbarController
import com.frozenlab.hack.databinding.ControllerIssueCreateBinding
import com.frozenlab.hack.databinding.ControllerMainBinding
import com.frozenlab.hack.websocket.VoiceWebSocketListener
import com.frozenlab.hack.websocket.getUnsafeOkHttpClientBuilder
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class IssueViewController: BaseController {

    companion object {
        private const val KEY_ISSUE_ID = "key_hack_issue_id"
    }

    private val issueId: Int

    constructor(issueId: Int): super() {
        this.issueId = issueId
        args.putInt(KEY_ISSUE_ID, issueId)
    }

    constructor(args: Bundle): super(args) {
        this.issueId = args.getInt(KEY_ISSUE_ID, -1)
    }

    private var issue: IssueDetails? = null

    override val binding: ControllerIssueCreateBinding get() = _binding!! as ControllerIssueCreateBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerIssueCreateBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        loadIssue()
    }

    private fun loadIssue() {
        apiRequest(
            hackApi.getIssueDetails(issueId),
            { issue = it }
        )
    }
}