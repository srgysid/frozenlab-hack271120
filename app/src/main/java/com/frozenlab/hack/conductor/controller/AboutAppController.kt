package com.frozenlab.hack.conductor.controller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.frozenlab.hack.BuildConfig
import com.frozenlab.hack.Preferences
import com.frozenlab.hack.R
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.conductor.controller.base.ToolbarController
import com.frozenlab.hack.databinding.ControllerAboutAppBinding

class AboutAppController: BaseController {

    constructor(): super()
    constructor(args: Bundle): super(args)

    override val binding: ControllerAboutAppBinding get() = _binding!! as ControllerAboutAppBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerAboutAppBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

        binding.textVersion.text = BuildConfig.VERSION_NAME

        binding.textUserAgreement.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Preferences.privacyPolicyUrl)))
        }
    }
}