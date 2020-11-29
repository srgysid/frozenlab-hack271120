package com.frozenlab.hack.conductor.controller

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import com.frozenlab.extensions.pushControllerHorizontal
import com.frozenlab.extensions.showToast
import com.frozenlab.hack.Preferences
import com.frozenlab.hack.R
import com.frozenlab.hack.api.models.OrderFlowType
import com.frozenlab.hack.api.models.OrderItem
import com.frozenlab.hack.api.models.OrderStatus
import com.frozenlab.hack.api.responses.RecognitionPartialResponse
import com.frozenlab.hack.api.responses.RecognitionResponse
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.databinding.ControllerMainBinding
import com.frozenlab.hack.databinding.PopupInputIssueByVoiceBinding
import com.frozenlab.hack.recycler.adapter.IssueItemAdapter
import com.frozenlab.hack.websocket.getUnsafeOkHttpClientBuilder
import com.frozenlab.ui.EndlessRecyclerViewScrollListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import retrofit2.Response
import timber.log.Timber
import java.io.*
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class MainController: BaseController {

    companion object {
        private const val TAB_ITEM_TAG_NEW = "new"
        private const val TAB_ITEM_TAG_IN  = "in"
        private const val TAB_ITEM_TAG_OUT = "out"

        private const val WEB_SOCKET_CLOSE_NORMAL_CODE   = 1000
        private const val WEB_SOCKET_CLOSE_NORMAL_REASON = "finished"
        private const val WEB_SOCKET_BUFFER_SIZE         = 8192

        private const val DELAY_BEFORE_EXIT = 3000L

        private const val KEY_SAVED_TAB = "key_hack_tab"
    }

    constructor(): super()
    constructor(args: Bundle): super(args)

    private val issuesList: ArrayList<OrderItem> = ArrayList()
    private lateinit var endlessScrollListener: EndlessRecyclerViewScrollListener

    private var currentTab: Tab = Tab.NEW

    private var webSocket:   WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var isReading:   Boolean = false

    private var recognizedPartial: String        = ""
    private var recognizedText:    StringBuilder = StringBuilder()

    private lateinit var popupBinding: PopupInputIssueByVoiceBinding
    private lateinit var popupDialog: AlertDialog

    private var askBeforeExit: Boolean = true

    override val binding: ControllerMainBinding get() = _binding!! as ControllerMainBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerMainBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

        createPopupVoiceDialog()

        binding.recyclerIssues.setHasFixedSize(true)
        binding.recyclerIssues.adapter = IssueItemAdapter(issuesList).apply {
            onClickListener = { position ->
                router.pushControllerHorizontal(OrderViewController(issuesList[position].id))
            }
        }

        endlessScrollListener = EndlessRecyclerViewScrollListener(binding.recyclerIssues.layoutManager!!)
        endlessScrollListener.setOnLoadMoreListener { page ->
            loadIssuesList(page)
        }
        binding.recyclerIssues.addOnScrollListener(endlessScrollListener)

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadIssuesList(1)
        }

        binding.bottomNavigationView.setOnNavigationItemSelectedListener(onBottomNavigationItemSelected)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        setupTabs()
        loadIssuesList()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord?.release()
        webSocket?.cancel()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SAVED_TAB, currentTab.name)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentTab = Tab.valueOf(savedInstanceState.getString(KEY_SAVED_TAB) ?: Tab.NEW.name)

    }
    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        currentTab = Tab.valueOf(savedViewState.getString(KEY_SAVED_TAB) ?: Tab.NEW.name)
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        outState.putString(KEY_SAVED_TAB, currentTab.name)
    }

    override fun handleBack(): Boolean {

        if(askBeforeExit) {
            mainActivity.showToast(R.string.ask_before_exit)
            askBeforeExit = false
            Handler().postDelayed({ askBeforeExit = true }, DELAY_BEFORE_EXIT)
            return true
        } else {
            exitProcess(0)
        }
    }

    private val onBottomNavigationItemSelected = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->

        when(menuItem.itemId) {
            R.id.bottom_menu_item_add -> {
                addNewIssue()
            }
        }
        true
    }

    private fun setupTabs() {

        if(binding.tabLayout.tabCount > 0)
            return

        binding.tabLayout.addTab(binding.tabLayout.newTab().apply {
            val tab = Tab.NEW
            customView = LayoutInflater.from(mainActivity).inflate(R.layout.view_tab_item, null, false)
            customView?.findViewById<TextView>(R.id.text_title)?.setText(tab.titleResId)
            customView?.setBackgroundResource(R.drawable.shape_toggle_button_selected)
            tag = tab.tag
        })

        binding.tabLayout.addTab(binding.tabLayout.newTab().apply {
            val tab = Tab.IN
            customView = LayoutInflater.from(mainActivity).inflate(R.layout.view_tab_item, null, false)
            customView?.findViewById<TextView>(R.id.text_title)?.setText(tab.titleResId)
            customView?.setBackgroundResource(R.drawable.shape_toggle_button_default)
            tag = tab.tag
        })

        binding.tabLayout.addTab(binding.tabLayout.newTab().apply {
            val tab = Tab.OUT
            customView = LayoutInflater.from(mainActivity).inflate(R.layout.view_tab_item, null, false)
            customView?.findViewById<TextView>(R.id.text_title)?.setText(tab.titleResId)
            customView?.setBackgroundResource(R.drawable.shape_toggle_button_default)
            tag = tab.tag
        })

        binding.tabLayout.addOnTabSelectedListener(tabSelectListener)
    }

    private val tabSelectListener = object: TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab ?: return

            when(tab.tag) {
                TAB_ITEM_TAG_NEW -> {
                    currentTab = Tab.NEW
                }

                TAB_ITEM_TAG_IN -> {
                    currentTab = Tab.IN
                }

                TAB_ITEM_TAG_OUT -> {
                    currentTab = Tab.OUT
                }
            }

            loadIssuesList()

            for(position in 0..binding.tabLayout.tabCount) {
                binding.tabLayout.getTabAt(position)?.let {
                    it.customView?.setBackgroundResource(if(it.isSelected) {
                        R.drawable.shape_toggle_button_selected
                    } else {
                        R.drawable.shape_toggle_button_default
                    })
                }
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    private fun loadIssuesList(page: Int? = null) {
        val statusId = if(currentTab == Tab.NEW) { OrderStatus.CREATED.id } else { null }
        val flowTypeId = when(currentTab) {
            Tab.IN -> { OrderFlowType.INCOMING.id }
            Tab.OUT -> { OrderFlowType.OUTGOING.id }
            else -> { null }
        }

        apiRequest(
            hackApi.getOrdersList(
                statusId,
                flowTypeId,
                page
            ),
            { response -> onIssuesListReceived(response) },
            showLoading = false
        )
    }

    private fun onIssuesListReceived(response: Response<ArrayList<OrderItem>>) {

        _binding ?: return

        val countTotal  = response.headers()["X-Pagination-Total-Count"]?.toInt() ?: 0
        val currentPage = response.headers()["X-Pagination-Current-Page"]?.toInt() ?: 0

        endlessScrollListener.setMaxCount(countTotal)

        if(binding.swipeRefreshLayout.isRefreshing)
            binding.swipeRefreshLayout.isRefreshing = false

        if(currentPage == 1) {
            issuesList.clear()
        }

        response.body()?.let { list ->
            issuesList.addAll(list)
            binding.recyclerIssues.adapter?.notifyDataSetChanged()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addNewIssue() {
        getPermissions {
            popupDialog.show()
            popupBinding.buttonRecord.setOnTouchListener(voiceTouchListener)
        }
    }

    private fun createPopupVoiceDialog() {

        popupBinding = PopupInputIssueByVoiceBinding.inflate(mainActivity.layoutInflater, null, false)
        popupDialog  = AlertDialog.Builder(mainActivity)
            .setView(popupBinding.root)
            .create()

        popupBinding.imageClose.setOnClickListener {
            popupDialog.dismiss()
        }

        popupBinding.buttonInput.setOnClickListener {
            popupDialog.dismiss()
            router.pushControllerHorizontal(OrderCreateController())
        }
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

        val listener = object : WebSocketListener() {

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                Timber.e("Message: $text")

                val response = try {
                    hackGson.fromJson(text, RecognitionResponse::class.java)
                } catch(e: Exception) {
                    null
                }

                val responsePartial = try {
                    hackGson.fromJson(text, RecognitionPartialResponse::class.java)
                } catch(e: Exception) {
                    null
                }

                responsePartial?.partial?.run {
                    recognizedPartial = this
                }

                response?.text?.run {
                    recognizedText.append(this).append(". ")
                    recognizedPartial = ""
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                super.onFailure(webSocket, t, response)
                mainActivity.handleThrowable(t)
            }
        }

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

                if(count <= 0) {
                    isReading = false
                } else {
                    if (webSocket?.send(buffer.toByteString()) != true) {
                        Timber.e("Web socket error")
                    }
                }
            }
        }.start()
    }

    private fun recordStart() {
        try {
            popupBinding.buttonRecord.setImageResource(R.drawable.ic_mic_active)
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

        popupBinding.buttonRecord.setImageResource(R.drawable.ic_mic_inactive)

        if(audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                audioRecord?.stop()
            } catch (e: Exception) {
                Timber.e(e.localizedMessage)
            }
        }

        Handler().postDelayed({
            try {
                webSocket?.close(WEB_SOCKET_CLOSE_NORMAL_CODE, WEB_SOCKET_CLOSE_NORMAL_REASON)
                webSocket = null
            } catch (e: Exception) {
                Timber.e(e.localizedMessage)
            }

            router.pushControllerHorizontal(OrderCreateController(recognizedText.toString() + recognizedPartial))
            //mainActivity.showToast(recognizedText.toString() + recognizedPartial, true)
            recognizedText.clear()
            popupDialog.dismiss()
        }, 500)
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

    enum class Tab(@StringRes val titleResId: Int, val tag: String) {
        NEW(R.string.tab_new, TAB_ITEM_TAG_NEW),
        IN(R.string.tab_in, TAB_ITEM_TAG_IN),
        OUT(R.string.tab_out, TAB_ITEM_TAG_OUT)
    }
}
