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
import com.frozenlab.hack.conductor.controller.base.ToolbarController
import com.frozenlab.welive.Preferences
import com.frozenlab.welive.R
import com.frozenlab.welive.api.isCorrectPhone
import com.frozenlab.hack.api.requests.LoginRequest
import com.frozenlab.welive.api.responses.NewAccessTokenResponse
import com.frozenlab.welive.api.toNormalizedPhoneNumber
import com.frozenlab.welive.databinding.ControllerLoginBinding
import com.redmadrobot.inputmask.MaskedTextChangedListener

class LoginController: ToolbarController {

    private companion object {
        private const val KEY_SAVED_PHONE    = "saved_phone"
        private const val KEY_SAVED_PASSWORD = "saved_password"
    }

    constructor(): super()
    constructor(args: Bundle): super(args)

    override val showToolbar: Boolean = false

    override val binding: ControllerLoginBinding get() = _binding!! as ControllerLoginBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerLoginBinding.inflate(inflater, container, false)

    }

    override fun onViewBound(view: View) {

        mainActivity.blockDrawer(true)

        binding.editablePhone.addTextWatcherForDisablingError()

        val listener = MaskedTextChangedListener(mainActivity.getString(R.string.mask_phone), binding.editablePhone.editText)
        binding.editablePhone.addTextChangedListener(listener)
        binding.editablePhone.onFocusChangeListener = listener

        binding.editablePassword.setOnKeyListener { _, _, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_UP && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.buttonSignIn.callOnClick()
                true
            } else {
                false
            }
        }

        binding.textRestorePassword.setOnClickListener(restorePasswordClickListener)
        binding.buttonSignIn.setOnClickListener(loginClickListener)
        binding.textSignUp.setOnClickListener(signUpClickListener)
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        outState.putString(KEY_SAVED_PHONE,    binding.editablePhone.text)
        outState.putString(KEY_SAVED_PASSWORD, binding.editablePassword.text)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        binding.editablePhone.text    = savedViewState.getString(KEY_SAVED_PHONE)
        binding.editablePassword.text = savedViewState.getString(KEY_SAVED_PASSWORD)
    }

    private val restorePasswordClickListener = View.OnClickListener {
        router.pushControllerHorizontal(RestorePasswordController(binding.editablePhone.text))
    }

    private val loginClickListener = View.OnClickListener {

        if (!binding.editablePhone.text.toString().isCorrectPhone()) {

            binding.editablePhone.isErrorEnabled = true
            binding.editablePhone.error = mainActivity.getString(R.string.available_phone_formats)

            mainActivity.showToast(R.string.error_phone_wrong)
            return@OnClickListener
        }

        val phone    = binding.editablePhone.text.toString().toNormalizedPhoneNumber()
        val password = binding.editablePassword.text.toString()

        val loginRequest = LoginRequest().apply {
            this.phone    = phone
            this.password = password
        }

        this.apiRequest(
            weLiveApi.login(loginRequest),
            { loginResponse -> onLoginSuccess(loginResponse) },
            { throwable -> onLoginFail(throwable) }
        )
    }

    private val signUpClickListener = View.OnClickListener {
        router.pushControllerHorizontal(RegistrationController(binding.editablePhone.text))
    }

    private fun onLoginSuccess(newAccessTokenResponse: NewAccessTokenResponse) {

        if(newAccessTokenResponse.accessToken.isEmpty()) {

            onLoginFail(Throwable(mainActivity.getString(R.string.error_login)))
            return
        }

        Preferences.loginPhone  = binding.editablePhone.text.toString().toNormalizedPhoneNumber()

        mainActivity.accessToken = newAccessTokenResponse.accessToken
        mainActivity.login()
    }

    private fun onLoginFail(throwable: Throwable) {

        Preferences.loginPhone  = ""
        mainActivity.accessToken = ""

        mainActivity.showToast(throwable.toApiError().message, true)
    }
}