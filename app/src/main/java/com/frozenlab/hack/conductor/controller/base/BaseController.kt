package com.frozenlab.hack.conductor.controller.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.frozenlab.api.ApiCommunicator
import com.frozenlab.extensions.hideKeyboard
import com.frozenlab.hack.MainActivity
import com.frozenlab.hack.api.HackApi
import com.frozenlab.hack.api.HackApiContext
import com.frozenlab.ui.conductor.controller.common.CommonController
import com.google.gson.Gson
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

abstract class BaseController: CommonController, HackApiContext, ApiCommunicator {

    constructor(): super()
    constructor(args: Bundle): super(args)

    protected val mainActivity:  MainActivity get() = activity!! as MainActivity
    override  val hackApi:       HackApi      get() = mainActivity.hackApi
    override  val hackGson:      Gson         get() = mainActivity.hackGson

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {

        mainActivity.hideKeyboard()

        return super.onCreateView(inflater, container, savedViewState)
    }

    override fun apiRequest(completable: Completable, successUnit: (() -> Unit)?, failUnit: ((throwable: Throwable) -> Unit)?, showLoading: Boolean) {
        mainActivity.apiRequest(completable, successUnit, failUnit, showLoading)
    }

    override fun <T> apiRequest(single: Single<T>, successUnit: ((param: T) -> Unit)?, failUnit: ((throwable: Throwable) -> Unit)?, showLoading: Boolean) {
        mainActivity.apiRequest(single, successUnit, failUnit, showLoading)
    }
}