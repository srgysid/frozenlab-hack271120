package com.frozenlab.hack.conductor.controller

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.frozenlab.api.ApiHolder
import com.frozenlab.api.toApiError
import com.frozenlab.extensions.*
import com.frozenlab.hack.BuildConfig
import com.frozenlab.hack.MainActivity
import com.frozenlab.hack.Preferences
import com.frozenlab.hack.R
import com.frozenlab.hack.api.ClassifierApi
import com.frozenlab.hack.api.models.*
import com.frozenlab.hack.api.requests.CreateOrderRequest
import com.frozenlab.hack.api.requests.TextRequest
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.databinding.ControllerIssueCreateBinding
import com.frozenlab.hack.databinding.PopupClassificationResultBinding
import com.frozenlab.hack.databinding.PopupInputIssueByVoiceBinding
import com.frozenlab.ui.addTextWatcherForDisablingError
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*
import kotlin.collections.ArrayList

class OrderCreateController: BaseController {

    companion object {
        private const val CLASSIFICATION_CHECK_INTERVAL = 300L
    }

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

    private val classificationHandler = Handler()
    private var classifiedCard = false
    private var classifiedOrder = false

    private var selectedTypeCard: TypeCard? = null
        set(value) {
            field = value
            value?.let {
                binding.spinnerTypeCard.spinner.setSelection(it.ordinal)
            }
        }
    private var selectedTypeOrder: Item? = null
        set(value) {
            field = value
            value?.let { item ->
                binding.spinnerTypeOrder.spinner.setSelection(mainActivity.typesOrders.indexOf(item))

                currentTypesMessage = mainActivity.typesMessages.filter { it.typeOrderId == item.id } as ArrayList<TypeMessage>
                configureTypeMessageSpinner(binding.spinnerTypeMessage.spinner) { type ->
                    selectedTypeMessage = type
                }
            }
        }

    private var currentTypesMessage: ArrayList<TypeMessage> = ArrayList()

    private var selectedTypeMessage: TypeMessage? = null
        set(value) {
            field = value
            value?.let {
                binding.spinnerTypeMessage.spinner.setSelection(currentTypesMessage.indexOf(it))
            }
        }
    private var selectedTypePerformers: TypePerformers? = null
        set(value) {
            field = value
            value?.let {
                binding.spinnerTypePerformer.spinner.setSelection(it.ordinal)

                if(value == TypePerformers.SELECTED) {
                    apiRequest(
                        hackApi.getPerformers(),
                        { list ->
                            binding.spinnerPerformer.isVisible = true
                            currentPerformers = list
                            configurePerformerSpinner(binding.spinnerPerformer.spinner) { performer ->
                                selectedPerformer = performer
                            }
                        },
                        showLoading = false
                    )
                } else {
                    binding.spinnerPerformer.isVisible = false
                }
            }
        }

    private var currentPerformers: ArrayList<Performer> = ArrayList()
    private var selectedPerformer: Performer? = null
        set(value) {
            field = value
            value?.let {
                binding.spinnerPerformer.spinner.setSelection(currentPerformers.indexOf(it))
            }
        }

    private var selectedPriority: OrderPriority? = null
        set(value) {
            field = value
            value?.let {
                binding.spinnerPriority.spinner.setSelection(it.ordinal)
            }
        }
    override val binding: ControllerIssueCreateBinding get() = _binding!! as ControllerIssueCreateBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerIssueCreateBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

        binding.editableTitle.addTextWatcherForDisablingError()
        binding.editableTitle.text = "Поручение"

        binding.editableRequiredDate.text = Date(Calendar.getInstance().time.time + 84600000 * 2).toFormattedString(mainActivity.getString(R.string.format_date))

        binding.bottomNavigationView.setOnNavigationItemSelectedListener(onBottomNavigationItemSelected)

        configureTypeCardSpinner(binding.spinnerTypeCard.spinner) { typeCard ->
            selectedTypeCard = typeCard
        }

        configureTypeOrderSpinner(binding.spinnerTypeOrder.spinner) { typeOrder ->
            selectedTypeOrder = typeOrder
        }

        configureTypeMessageSpinner(binding.spinnerTypeMessage.spinner) { typeMessage ->
            selectedTypeMessage = typeMessage
        }

        configureTypePerformersSpinner(binding.spinnerTypePerformer.spinner) { typePerformers ->
            selectedTypePerformers = typePerformers
        }

        configurePerformerSpinner(binding.spinnerPerformer.spinner) { performer ->
            selectedPerformer = performer
        }

        configurePrioritySpinner(binding.spinnerPriority.spinner) { priority ->
            selectedPriority = priority
        }

        textOrder?.let {
            classificationHandler.post(classificationChecker)
            classifyTypeCard()
            classifyTypeOrder()
            binding.editableDescription.text = it
        }

        binding.imageDatePicker.setOnClickListener {
            pickDate()
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

            this.title         = binding.editableTitle.text.toString()
            this.description   = binding.editableDescription.text.toString()
            this.typeCards     = selectedTypeCard?.id ?: -1
            this.typeMessageId = selectedTypeMessage?.id ?: -1
            this.typePerformer = selectedTypePerformers?.id ?: -1
            this.priority      = selectedPriority?.id ?: -1
            if(selectedTypePerformers == TypePerformers.SELECTED) {
                this.performers = intArrayOf(selectedPerformer?.id ?: -1)
            }
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

    private fun configureTypeCardSpinner(spinner: Spinner, onItemSelected: ((TypeCard?) -> Unit)? = null) {

        spinner.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            TypeCard.values().map { mainActivity.getString(it.titleId) }
        )

        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected?.invoke(TypeCard.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        selectedTypeCard?.let { typeCard ->
            spinner.setSelection(typeCard.ordinal)
        }
    }

    private fun configureTypeOrderSpinner(spinner: Spinner, onItemSelected: ((Item?) -> Unit)? = null) {

        spinner.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            mainActivity.typesOrders.map { it.title }
        )

        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected?.invoke(mainActivity.typesOrders[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        selectedTypeOrder?.let { typeOrder ->
            spinner.setSelection(mainActivity.typesOrders.indexOf(typeOrder))
        }
    }

    private fun configureTypeMessageSpinner(spinner: Spinner, onItemSelected: ((TypeMessage?) -> Unit)? = null) {

        spinner.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            currentTypesMessage.map { it.title }
        )

        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected?.invoke(currentTypesMessage[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        selectedTypeMessage?.let { typeMessages ->
            spinner.setSelection(currentTypesMessage.indexOf(typeMessages))
        }
    }

    private fun configureTypePerformersSpinner(spinner: Spinner, onItemSelected: ((TypePerformers?) -> Unit)? = null) {

        spinner.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            TypePerformers.values().map { mainActivity.getString(it.titleId) }
        )

        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected?.invoke(TypePerformers.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        selectedTypePerformers?.let { item ->
            spinner.setSelection(item.ordinal)
        }
    }

    private fun configurePerformerSpinner(spinner: Spinner, onItemSelected: ((Performer?) -> Unit)? = null) {

        spinner.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            currentPerformers.map { it.name.shortFullName }
        )

        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected?.invoke(currentPerformers[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        selectedPerformer?.let { performer ->
            spinner.setSelection(currentPerformers.indexOf(performer))
        }
    }

    private fun configurePrioritySpinner(spinner: Spinner, onItemSelected: ((OrderPriority?) -> Unit)? = null) {

        spinner.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            OrderPriority.values().map { mainActivity.getString(it.titleId) }
        )

        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected?.invoke(OrderPriority.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        selectedPriority?.let { item ->
            spinner.setSelection(item.ordinal)
        }
    }

    private fun pickDate() {

        val calendar = Calendar.getInstance()

        val pickerBuilder = MaterialDatePicker.Builder.datePicker()

        pickerBuilder.setSelection(calendar.time.time)

        val picker = pickerBuilder.build()
        picker.show(mainActivity.supportFragmentManager, picker.toString())

        picker.addOnPositiveButtonClickListener { millis ->
            binding.editableRequiredDate.text = Date(millis).toFormattedString(mainActivity.getString(R.string.format_date))
        }
    }

    private val classificationChecker = object: Runnable {
        override fun run() {

            if(classifiedCard && classifiedOrder) {

                showClassificationResult()

                classificationHandler.removeCallbacks(this)
                return
            }

            classificationHandler.postDelayed(this, CLASSIFICATION_CHECK_INTERVAL)
        }
    }

    private fun showClassificationResult() {
        val popupBinding = PopupClassificationResultBinding.inflate(mainActivity.layoutInflater, null, false)
        val dialog  = AlertDialog.Builder(mainActivity)
            .setView(popupBinding.root)
            .create()

        popupBinding.imageClose.setOnClickListener {
            dialog.dismiss()
        }

        popupBinding.buttonContinue.setOnClickListener {
            dialog.dismiss()
        }

        popupBinding.textOrder.text = textOrder

        selectedTypeCard?.let {
            popupBinding.textTypeCard.setText(it.titleId)
        }

        selectedTypeOrder?.let {
            popupBinding.textTypeOrder.text = it.title
        }

        dialog.show()
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
                selectedTypeCard = TypeCard.values().find { it.id == topValue.value }
                classifiedCard = true
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
                classifiedOrder = true
            },
            showLoading = false
        )
    }
}