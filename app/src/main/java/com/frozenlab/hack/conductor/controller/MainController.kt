package com.frozenlab.hack.conductor.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import com.frozenlab.extensions.pushControllerHorizontal
import com.frozenlab.hack.R
import com.frozenlab.hack.api.models.IssueItem
import com.frozenlab.hack.conductor.controller.base.BaseController
import com.frozenlab.hack.databinding.ControllerMainBinding
import com.frozenlab.hack.recycler.adapter.IssueItemAdapter
import com.frozenlab.ui.EndlessRecyclerViewScrollListener
import com.google.android.material.tabs.TabLayout
import retrofit2.Response
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

class MainController: BaseController {

    companion object {
        private const val TAB_ITEM_TAG_NEW = "new"
        private const val TAB_ITEM_TAG_IN  = "in"
        private const val TAB_ITEM_TAG_OUT = "out"
    }

    constructor(): super()
    constructor(args: Bundle): super(args)

    private val issuesList: ArrayList<IssueItem> = ArrayList()
    private lateinit var endlessScrollListener: EndlessRecyclerViewScrollListener

    private var currentTab: Tab = Tab.NEW

    override val binding: ControllerMainBinding get() = _binding!! as ControllerMainBinding

    override fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup): ViewBinding {
        return ControllerMainBinding.inflate(inflater, container, false)
    }

    override fun onViewBound(view: View) {

        binding.recyclerIssues.setHasFixedSize(true)
        binding.recyclerIssues.adapter = IssueItemAdapter(issuesList).apply {
            onClickListener = { position ->
                router.pushControllerHorizontal(IssueViewController(issuesList[position].id))
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
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        loadIssuesList()
    }

    private fun setupTabs() {


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
        apiRequest(
            hackApi.getIssuesList(page),
            { response -> onIssuesListReceived(response) },
            showLoading = false
        )
    }

    private fun onIssuesListReceived(response: Response<ArrayList<IssueItem>>) {

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

    enum class Tab(@StringRes val titleResId: Int, val tag: String) {
        NEW(R.string.tab_new, TAB_ITEM_TAG_NEW),
        IN(R.string.tab_in, TAB_ITEM_TAG_IN),
        OUT(R.string.tab_out, TAB_ITEM_TAG_OUT)
    }
}
