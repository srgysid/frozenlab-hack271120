package com.frozenlab.hack.conductor.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.frozenlab.hack.api.models.OrderDetails
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.databinding.ControllerIssueCreateBinding
import com.frozenlab.hack.databinding.ControllerIssueViewBinding

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

    private var issue: OrderDetails? = null

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
            hackApi.getOrderDetails(issueId),
            { issue = it }
        )
    }
}