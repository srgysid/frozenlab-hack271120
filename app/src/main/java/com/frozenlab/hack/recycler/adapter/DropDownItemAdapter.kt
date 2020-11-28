package com.frozenlab.hack.recycler.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.frozenlab.hack.api.models.Item
import com.frozenlab.hack.databinding.AdapterDropDownItemBinding

class DropDownItemAdapter(private val items: ArrayList<out Item>): RecyclerView.Adapter<DropDownItemAdapter.DropDownItemViewHolder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DropDownItemViewHolder {
        return DropDownItemViewHolder(AdapterDropDownItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: DropDownItemViewHolder, position: Int) {

        val item = items[position]
        holder.binding.text.text = item.toString()
    }

    var onClickListener: ((position: Int) -> Unit)? = null

    inner class DropDownItemViewHolder(val binding: AdapterDropDownItemBinding): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onClickListener?.invoke(adapterPosition)
            }
        }
    }

}