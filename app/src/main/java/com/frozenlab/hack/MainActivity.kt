package com.frozenlab.hack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.frozenlab.api.ApiCommunicator
import com.frozenlab.api.ApiError
import com.frozenlab.api.ApiHolder
import com.frozenlab.api.toApiError
import com.frozenlab.extensions.*
import com.frozenlab.hack.api.HackApiContext
import com.frozenlab.hack.api.HackApi
import com.frozenlab.hack.api.requests.FCMRequest
import com.frozenlab.hack.conductor.controller.LoginController
import com.frozenlab.hack.conductor.controller.MainController
import com.frozenlab.hack.databinding.ActivityMainBinding
import com.frozenlab.welive.api.models.UserProfile
import com.google.gson.Gson
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), HackApiContext, ApiCommunicator {

    companion object {
        const val REQUEST_PERMISSIONS_CODE = 6861
        const val USER_DATA_LOADED_CHECK_INTERVAL = 500L // Milliseconds

        const val INTENT_NEW_FCM_TOKEN     = "HackNewFCMToken"
        const val INTENT_NOTIFICATION_DATA = "HackWeLiveNotificationData"

        const val SAVED_CURRENT_USER_PROFILE  = "key_hack_user_profile"
    }

    var currentUserProfile: UserProfile? = null
        set(value) {
            field = value
            updateUserProfileViews()
        }

    private var accessToken: String = Preferences.accessToken
        set(value) {
            field = value
            Preferences.accessToken = value
            apiHolder.setHeader("Authorization", "Bearer $value")
        }

    private val apiHolder = ApiHolder(
        HackApi::class.java,
        Preferences.apiURL,
        hashMapOf(Pair("Authorization", "Bearer $accessToken")),
        hashMapOf(),
        Preferences.jsonDateFormat,
        Preferences.okHttpSocketTimeOut,
        BuildConfig.DEBUG
    )

    override val hackApi:  HackApi = apiHolder.api as HackApi
    override val hackGson: Gson    = apiHolder.gson

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private lateinit var router: Router

    private var afterPermissionGrantedCallback: (() -> Unit)? = null
    private val loadingIndicator: AlertDialog by lazy {
        AlertDialog.Builder(this)
            .setView(ProgressBar(this))
            .create()
    }

    private var loginFailed = false
    private var userProfileLoaded  = false
    private val userDataLoadedHandler: Handler = Handler()

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        //setSupportActionBar(binding.coordinator.toolbar)

        //configureDrawer()

        router = Conductor.attachRouter(
            this,
            binding.coordinator.controllerContainer,
            savedInstanceState
        )

        if(!router.hasRootController()) {
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userDataLoadedHandler.removeCallbacks(userDataLoadedChecker)
        compositeDisposable.clear()
    }


    override fun onStart() {
        super.onStart()

        val intentFilter = IntentFilter().apply {
            addAction(INTENT_NEW_FCM_TOKEN)
            addAction(INTENT_NOTIFICATION_DATA)
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastMessagesReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()

        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastMessagesReceiver)
    }

    /*override fun onSupportNavigateUp(): Boolean {

        if(router.backstackSize > 1)
            onBackPressed()
        else
            openDrawer()

        return super.onSupportNavigateUp()
    }*/

    override fun onBackPressed() {

        if(!router.handleBack()) {
            super.onBackPressed()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        if(ev == null)
            return super.dispatchTouchEvent(ev)

        if(ev.action == MotionEvent.ACTION_DOWN) {

            val view = currentFocus
            if(view is EditText) {

                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)

                if (!outRect.contains(ev.x.toInt(), ev.y.toInt())) {
                    view.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(SAVED_CURRENT_USER_PROFILE, hackGson.toJson(currentUserProfile))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.getString(SAVED_CURRENT_USER_PROFILE)?.let { json ->
            if (json.isNotEmpty()) {
                currentUserProfile = hackGson.fromJson(json, UserProfile::class.java)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(REQUEST_PERMISSIONS_CODE == requestCode) {

            for((index, _) in permissions.withIndex()) {
                if(grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    this.showToast(R.string.ask_permissions, true)
                    afterPermissionGrantedCallback = null
                    return
                }
            }

            afterPermissionGrantedCallback?.invoke()
            afterPermissionGrantedCallback = null
        }
    }

    override fun apiRequest(
        completable: Completable,
        successUnit: (() -> Unit)?,
        failUnit: ((throwable: Throwable) -> Unit)?,
        showLoading: Boolean
    ) {

        if(showLoading) {
            showLoadingIndicator(true)
        }

        compositeDisposable.add(
            completable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    if (showLoading) {
                        showLoadingIndicator(false)
                    }
                }
                .subscribe(successUnit, failUnit ?: { throwable -> onReceiveFail(throwable) })
        )
    }

    override fun <T> apiRequest(
        single: Single<T>,
        successUnit: ((param: T) -> Unit)?,
        failUnit: ((throwable: Throwable) -> Unit)?,
        showLoading: Boolean
    ) {

        if(showLoading) {
            showLoadingIndicator(true)
        }

        compositeDisposable.add(
            single
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    if (showLoading) {
                        showLoadingIndicator(false)
                    }
                }
                .subscribe(successUnit, failUnit ?: { throwable -> onReceiveFail(throwable) })
        )
    }

    //
    // Public
    //

    fun requestPermissionsIfNeed(
        requiredPermissions: ArrayList<String>,
        afterGrantedCallback: (() -> Unit)? = null
    ): Boolean {

        val notGrantedPermissions = this.getNotGrantedPermissions(requiredPermissions)

        if(notGrantedPermissions.size > 0) {
            afterPermissionGrantedCallback = afterGrantedCallback
            ActivityCompat.requestPermissions(
                this,
                notGrantedPermissions.toTypedArray(),
                REQUEST_PERMISSIONS_CODE
            )
            return true
        }

        return false
    }

    fun blockDrawer(block: Boolean) {
        if(block)
            binding.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        else
            binding.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    fun login() {

        loginFailed        = false
        userProfileLoaded  = false

        userDataLoadedHandler.post(userDataLoadedChecker)

        if(accessToken.isNotBlank()) {

            applyFCMToken()
            getCurrentUserProfileAndAccounts()

        } else {
            loginFailed = true
        }
    }

    private fun logout() {

        removeFCMToken()

        // This delay is needed before removing the access token in order to give time to the method above
        Handler().postDelayed({
            this.accessToken = ""
        }, 300L)

        currentUserProfile  = null

        blockDrawer(true)

        router.setRootFade(LoginController())
    }

    //
    // Private

    private val broadcastMessagesReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            when(intent?.action) {

                INTENT_NEW_FCM_TOKEN -> {
                    applyFCMToken()
                }

                INTENT_NOTIFICATION_DATA -> {

                }
            }
        }
    }

    private val userDataLoadedChecker = object: Runnable {
        override fun run() {

            if(loginFailed) {

                userDataLoadedHandler.removeCallbacks(this)

                binding.coordinator.initProgressBar.isVisible = false

                if(router.backstack.isEmpty() || (router.backstack.first().controller !is LoginController)) {
                    router.setRootInstant(LoginController())
                }

                return
            }

            if(userProfileLoaded) {

                binding.coordinator.initProgressBar.isVisible = false

                userDataLoadedHandler.removeCallbacks(this)

                router.setRootFade(MainController())
                blockDrawer(false)

                return
            }

            userDataLoadedHandler.postDelayed(this, USER_DATA_LOADED_CHECK_INTERVAL)
        }
    }


    private fun start() {
        binding.coordinator.initProgressBar.isVisible = true
        login()
    }

    private fun applyFCMToken() {
        val token = Preferences.fcmToken.ifEmpty { null }
        if(!token.isNullOrBlank())
            sendFCMTokenToServer(token, token)
    }

    private fun removeFCMToken() {
        val token = Preferences.fcmToken.ifEmpty { null }
        if(!token.isNullOrBlank())
            sendFCMTokenToServer(token, null)
    }

    private fun sendFCMTokenToServer(oldToken: String?, newToken: String?) {

        val fcmRequest = FCMRequest().apply {
            this.newToken = newToken
            this.oldToken = oldToken
        }

        apiRequest(
            hackApi.sendFCMToken(fcmRequest),
            successUnit = {
                if(!fcmRequest.newToken.isNullOrEmpty())
                    Preferences.fcmToken = fcmRequest.newToken!!
            },
            showLoading = false
        )
    }

    private fun updateUserProfileViews() {
        binding.menuItemProfile.title = currentUserProfile?.name?.firstThirdName
    }

    private fun getCurrentUserProfileAndAccounts() {

        this.apiRequest(
            hackApi.getUserProfile(),
            { userProfile ->
                userProfileLoaded  = true
                currentUserProfile = userProfile
            },
            { handleThrowable(it, true) },
            false
        )
    }

    private fun closeDrawer() {
        binding.drawer.closeDrawer(GravityCompat.START)
    }

    private fun openDrawer() {
        binding.drawer.openDrawer(GravityCompat.START)
    }

    private fun configureDrawer() {

        binding.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        // Close button click
        binding.buttonMenuClose.setOnClickListener { closeDrawer() }

        // Profile
        binding.menuItemProfile.setOnClickListener {

            closeDrawer()
        }

        // AboutApp
        binding.menuItemAboutApp.hint = "${getString(R.string.version)} ${com.frozenlab.extensions.BuildConfig.VERSION_NAME}"
        binding.menuItemAboutApp.setOnClickListener {
            router.pushControllerHorizontal(AboutAppController())
            closeDrawer()
        }

        // SignOut
        binding.menuItemSignOut.setOnClickListener {
            logout()
        }
    }

    private fun onReceiveFail(throwable: Throwable) {
        handleThrowable(throwable, false)
    }

    private fun handleThrowable(throwable: Throwable, exitOnNetworkError: Boolean = false) {

        val apiError = throwable.toApiError()

        when(apiError.code) {
            ApiError.ERROR_NETWORK_CONNECTION_FAILED -> {
                if (!exitOnNetworkError) {
                    this.showToast(R.string.error_network_connection_failed)
                } else {

                    if(!router.hasRootController()) {
                        userDataLoadedHandler.removeCallbacks(userDataLoadedChecker)
                        binding.coordinator.initProgressBar.isVisible = false
                        binding.coordinator.layoutRestoreConnection.isVisible = true
                        binding.coordinator.buttonRepeat.setOnClickListener {
                            binding.coordinator.layoutRestoreConnection.isVisible = false
                            start()
                        }

                    } else {
                        //this.showToast(R.string.error_network_connection_failed)
                        exitProcess(0)
                    }
                }
            }
            ApiError.ERROR_HTTP_UNAUTHORIZED -> {
                //if (accessToken.isNotEmpty()) {
                    //logout()
                //}
                this.showToast(R.string.error_login)
            }
            else -> this.showToast(apiError.message, true)
        }
    }

    private fun showLoadingIndicator(show: Boolean) {

        if (show) {
            loadingIndicator.show()
            loadingIndicator.window?.setBackgroundDrawable(null)
        } else {
            loadingIndicator.hide()
        }
    }

}