package com.frozenlab.hack.conductor.controller

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.frozenlab.api.ApiHolder
import com.frozenlab.api.toApiError
import com.frozenlab.extensions.showToast
import com.frozenlab.hack.BuildConfig
import com.frozenlab.hack.Preferences
import com.frozenlab.hack.R
import com.frozenlab.hack.api.ClassifierApi
import com.frozenlab.hack.api.HackApi
import com.frozenlab.hack.api.models.Item
import com.frozenlab.hack.api.models.OrderItem
import com.frozenlab.hack.api.models.TypeCards
import com.frozenlab.hack.api.models.TypeMessage
import com.frozenlab.hack.api.requests.CreateOrderRequest
import com.frozenlab.hack.api.requests.TextRequest
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.databinding.ControllerIssueCreateBinding
import com.frozenlab.welive.api.models.UserProfile
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*
import kotlin.collections.ArrayList

class OrderCreateController: BaseController {

    private var textOrder: String? = null

    constructor(text: String? = null): super() {
        this.textOrder = text
    }
    constructor(args: Bundle): super(args)

    private val apiCardHolder = ApiHolder(
        ClassifierApi::class.java,
        Preferences.apiCardURL,
        hashMapOf(),
        hashMapOf(),
        Preferences.jsonDateFormat,
        Preferences.okHttpSocketTimeOut,
        BuildConfig.DEBUG
    )

    private val cardApi = apiCardHolder.api as ClassifierApi

    private val apiOrderHolder = ApiHolder(
        ClassifierApi::class.java,
        Preferences.apiOrderURL,
        hashMapOf(),
        hashMapOf(),
        Preferences.jsonDateFormat,
        Preferences.okHttpSocketTimeOut,
        BuildConfig.DEBUG
    )

    private val orderApi = apiOrderHolder.api as ClassifierApi

    private var selectedTypeCard: TypeCards? = null
        set(value) {
            field = value
            value?.let {
                binding.editableTypeCard.text = mainActivity.getString(it.titleId)
            }
        }
    private var selectedTypeOrder: Item? = null
        set(value) {
            field = value
            value?.let {
                binding.editableTypeOrder.text = it.title
            }
        }

    private var selectedTypeMessage: TypeMessage? = null

    override val binding: ControllerIssueCreateBinding get() = _binding!! as ControllerIssueCreateBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerIssueCreateBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

        binding.bottomNavigationView.setOnNavigationItemSelectedListener(onBottomNavigationItemSelected)

        textOrder?.let {
            classifyTypeCard()
            classifyTypeOrder()
        }
    }

    private val onBottomNavigationItemSelected = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->

        when(menuItem.itemId) {
            R.id.bottom_menu_item_back -> {
                router.handleBack()
            }

            R.id.bottom_menu_item_confirm -> {
                createOrder()
            }
        }
        true
    }

    private fun createOrder() {

        if(!checkFields()) {
            mainActivity.showToast(mainActivity.getString(R.string.error_fields_wrong))
            return
        }

        binding.bottomNavigationView.isEnabled = false

        val request = CreateOrderRequest().apply {

            this.title             = binding.editableTitle.text.toString()
            this.description       = binding.editableDescription.text.toString()
        }

        val files: ArrayList<MultipartBody.Part> = ArrayList()

        /*
        for(uri in imageUriList) {

            uri?.also { thisUri ->

                applicationContext!!.contentResolver?.let { contentResolver ->

                    val byteArray = contentResolver.getJpegByteArrayFromUri(thisUri, MAX_UPLOAD_IMAGE_SIZE)

                    imageFiles.add(
                        MultipartBody.Part.createFormData(
                            IMAGE_VARIABLE_NAME,
                            "image",
                            byteArray.toRequestBody()
                        )
                    )
                }
            }
        }
        */
        apiRequest(
            hackApi.createOrder(
                Gson().toJson(request).toRequestBody(),
                files
            ),
            {
                router.popCurrentController()
            },
            {
                mainActivity.showToast(it.toApiError().message)
                binding.bottomNavigationView.isEnabled = true
            }
        )
    }

    private fun checkFields(): Boolean {

        var result = true

        if(binding.editableTitle.text.isNullOrBlank()) {
            binding.editableTitle.isErrorEnabled = true
            binding.editableTitle.error = mainActivity.getString(R.string.error_field_blank)
            result = false
        }

        return result
    }

    private fun classifyTypeCard() {

        val request = TextRequest().apply {
            text = textOrder!!
        }
        apiRequest(
            cardApi.getValues(request),
            { list ->
                if(list.size == 0)
                    return@apiRequest
                val topValue = list[0]
                selectedTypeCard = TypeCards.values().find { it.id == topValue.value }
            },
            showLoading = false
        )
    }

    private fun classifyTypeOrder() {

        val request = TextRequest().apply {
            text = textOrder!!
        }

        apiRequest(
            orderApi.getValues(request),
            { list ->
                if(list.size == 0)
                    return@apiRequest
                val topValue = list[0]
                selectedTypeOrder = mainActivity.typesOrders.find { it.id == topValue.value }
            },
            showLoading = false
        )
    }
}