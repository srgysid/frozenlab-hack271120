package com.frozenlab.hack.conductor.controller

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.frozenlab.api.toApiError
import com.frozenlab.extensions.pushControllerHorizontal
import com.frozenlab.extensions.showToast
import com.frozenlab.hack.Preferences
import com.frozenlab.hack.R
import com.frozenlab.hack.conductor.controller.base.ToolbarController
import com.frozenlab.hack.api.requests.LoginRequest
import com.frozenlab.hack.api.responses.NewAccessTokenResponse
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.databinding.ControllerLoginBinding
import com.frozenlab.ui.addTextWatcherForDisablingError

class LoginController: BaseController {

    private companion object {
        private const val KEY_SAVED_LOGIN    = "key_hack_login"
        private const val KEY_SAVED_PASSWORD = "key_hack_password"
    }

    constructor(): super()
    constructor(args: Bundle): super(args)

    override val binding: ControllerLoginBinding get() = _binding!! as ControllerLoginBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerLoginBinding.inflate(inflater, container, false)

    }

    override fun onViewBound(view: View) {

        mainActivity.blockDrawer(true)

        //binding.editableLogin.addTextWatcherForDisablingError(binding.wrapperLogin)

        binding.editablePassword.setOnKeyListener { _, _, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_UP && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.buttonSignIn.callOnClick()
                true
            } else {
                false
            }
        }

        //binding.textRestorePassword.setOnClickListener(restorePasswordClickListener)
        binding.buttonSignIn.setOnClickListener(loginClickListener)
        //binding.textSignUp.setOnClickListener(signUpClickListener)
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        outState.putString(KEY_SAVED_LOGIN,    binding.editableLogin.text.toString())
        outState.putString(KEY_SAVED_PASSWORD, binding.editablePassword.text.toString())
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        binding.editableLogin.setText(savedViewState.getString(KEY_SAVED_LOGIN))
        binding.editablePassword.setText(savedViewState.getString(KEY_SAVED_PASSWORD))
    }

    private val restorePasswordClickListener = View.OnClickListener {
        //router.pushControllerHorizontal(RestorePasswordController(binding.editablePhone.text))
    }

    private val loginClickListener = View.OnClickListener {

        if(binding.editableLogin.text.isNullOrBlank()) {
            mainActivity.showToast(R.string.error_field_blank)
            return@OnClickListener
        }

        val login    = binding.editableLogin.text.toString()
        val password = binding.editablePassword.text.toString()

        val loginRequest = LoginRequest().apply {
            this.login    = login
            this.password = password
        }

        this.apiRequest(
            hackApi.login(loginRequest),
            { loginResponse -> onLoginSuccess(loginResponse) },
            { throwable -> onLoginFail(throwable) }
        )
    }

    private val signUpClickListener = View.OnClickListener {
        //router.pushControllerHorizontal(RegistrationController(binding.editablePhone.text))
    }

    private fun onLoginSuccess(newAccessTokenResponse: NewAccessTokenResponse) {

        if(newAccessTokenResponse.accessToken.isEmpty()) {

            onLoginFail(Throwable(mainActivity.getString(R.string.error_login)))
            return
        }

        mainActivity.login(newAccessTokenResponse.accessToken)
    }

    private fun onLoginFail(throwable: Throwable) {
        mainActivity.showToast(throwable.toApiError().message, true)
    }
}