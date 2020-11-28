package com.frozenlab.hack.custom.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.frozenlab.hack.R
import com.frozenlab.hack.api.models.Item
import com.frozenlab.hack.databinding.PopupDropDownListBinding
import com.frozenlab.hack.recycler.adapter.DropDownItemAdapter
import com.frozenlab.ui.EndlessRecyclerViewScrollListener

class DropDownList(context: Context): PopupWindow() {

    private val popUpEndlessScrollListener: EndlessRecyclerViewScrollListener

    private val itemsList: ArrayList<Item>
    private val itemHeight: Int

    private var _binding: PopupDropDownListBinding? = null
    private val binding: PopupDropDownListBinding get() = _binding!!

    init {

        _binding = PopupDropDownListBinding.inflate(LayoutInflater.from(context), null, false)

        this.contentView = binding.root
        this.isOutsideTouchable = true
        this.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        this.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        itemsList = ArrayList()

        binding.imageMoreItemsTop.isVisible = false
        binding.imageMoreItemsBottom.isVisible = false

        binding.recycler.setHasFixedSize(true)

        binding.recycler.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        binding.recycler.adapter = DropDownItemAdapter(itemsList)

        popUpEndlessScrollListener = EndlessRecyclerViewScrollListener(binding.recycler.layoutManager!!)

        popUpEndlessScrollListener.setOnScrolledToStartListener {
            binding.imageMoreItemsTop.isVisible = false
        }

        popUpEndlessScrollListener.setOnScrolledToEndListener {
            binding.imageMoreItemsBottom.isVisible = false
        }

        popUpEndlessScrollListener.setOnScrolledInMiddleListener {
            binding.imageMoreItemsTop.isVisible = true
            binding.imageMoreItemsBottom.isVisible = true
        }

        this.setOnDismissListener {
            itemsList.clear()
            binding.recycler.adapter?.notifyDataSetChanged()
            popUpEndlessScrollListener.resetState()
        }

        binding.recycler.addOnScrollListener(popUpEndlessScrollListener)

        itemHeight = context.resources?.getDimensionPixelSize(R.dimen.height_drop_down_list_item) ?: 0
    }

    override fun showAsDropDown(view: View) {
        super.showAsDropDown(view)
        this.update(view)
    }

    fun update(view: View?) {

        view?.also {
            val size = calculateSize(it)
            this.update(it, size.width, size.height)
        } ?: also {
            this.update()
        }
    }

    fun setNewItemsList(list: ArrayList<out Item>, onClickListener: (position: Int) -> Unit) {

        itemsList.clear()
        itemsList.addAll(list)
        binding.recycler.adapter?.notifyDataSetChanged()

        (binding.recycler.adapter as DropDownItemAdapter).onClickListener = onClickListener
    }

    fun appendItems(list: ArrayList<out Item>) {
        itemsList.addAll(list)
        binding.recycler.adapter?.notifyDataSetChanged()
    }

    fun getItem(position: Int): Item? {
        return if(position in 0 until itemsList.size) {
            itemsList[position]
        } else { null }
    }

    fun setMaxItemsCount(maxCount: Int) {
        popUpEndlessScrollListener.setMaxCount(maxCount)
    }

    fun setOnLoadMoreListener(listener: (page: Int) -> Unit) {
        popUpEndlessScrollListener.resetState()
        popUpEndlessScrollListener.setOnLoadMoreListener(listener)
    }

    private fun calculateSize(view: View): Size {

        val density = Resources.getSystem().displayMetrics.density

        val windowHeight    = (itemHeight * itemsList.size + 40 * density).toInt()
        val windowMaxHeight = this.getMaxAvailableHeight(view)

        val width  = view.measuredWidth
        val height = if(windowHeight > windowMaxHeight) {
            binding.imageMoreItemsBottom.isVisible = true
            windowMaxHeight
        } else {
            binding.imageMoreItemsBottom.isVisible = false
            windowHeight
        }

        return Size(width, height)
    }
}