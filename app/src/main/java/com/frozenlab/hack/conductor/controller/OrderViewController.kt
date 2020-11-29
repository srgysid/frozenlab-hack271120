package com.frozenlab.hack.conductor.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.frozenlab.extensions.toFormattedString
import com.frozenlab.hack.R
import com.frozenlab.hack.api.models.OrderDetails
import com.frozenlab.hack.api.models.OrderPriority
import com.frozenlab.hack.api.models.TypeCard
import com.frozenlab.hack.api.models.TypePerformers
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.databinding.ControllerIssueViewBinding

class OrderViewController: BaseController {

    companion object {
        private const val KEY_ISSUE_ID = "key_hack_issue_id"
    }

    private val orderId: Int

    constructor(orderId: Int): super() {
        this.orderId = orderId
        args.putInt(KEY_ISSUE_ID, orderId)
    }

    constructor(args: Bundle): super(args) {
        this.orderId = args.getInt(KEY_ISSUE_ID, -1)
    }

    private var order: OrderDetails? = null

    override val binding: ControllerIssueViewBinding get() = _binding!! as ControllerIssueViewBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerIssueViewBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        loadIssue()
    }

    private fun loadIssue() {
        apiRequest(
            hackApi.getOrderDetails(orderId),
            { onOrderReceived(it) }
        )
    }

    private fun onOrderReceived(order: OrderDetails) {
        this.order = order

        val formatDate = mainActivity.getString(R.string.format_date)
        binding.textTitle.text = order.title
        binding.textDescription.text = order.description

        binding.textCreatedAt.text = mainActivity.getString(R.string.template_created_at, order.closedAt.toFormattedString(formatDate))

        binding.textPlannedAt.isVisible = order.requiredDate?.let {
            binding.textPlannedAt.text = mainActivity.getString(R.string.template_planned_at, it.toFormattedString(formatDate))
            true
        } ?: false

        binding.textFinishedAt.isVisible = order.factDate?.let {
            binding.textFinishedAt.text = mainActivity.getString(R.string.template_finished_at, it.toFormattedString(formatDate))
            true
        } ?: false

        binding.textTypeCard.isVisible = TypeCard.values().find { it.id == order.typeCards }?.let {
            binding.textTypeCard.text = mainActivity.getString(it.titleId)
            true
        } ?: false

        binding.textTypePerformer.isVisible = TypePerformers.values().find { it.id == order.typePerformers }?.let {
            binding.textTypePerformer.text = mainActivity.getString(it.titleId)
            true
        } ?: false

        binding.textPriority.isVisible = OrderPriority.values().find { it.id == order.priority }?.let {
            binding.textPriority.text = mainActivity.getString(it.titleId)
            true
        } ?: false

        binding.textDepartment.isVisible = if(order.departmentName.isNotBlank()) {
            binding.textDepartment.text = order.departmentName
            true
        } else { false }
    }
}