package com.frozenlab.hack.recycler.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.frozenlab.hack.api.models.OrderItem
import com.frozenlab.hack.databinding.AdapterIssueItemBinding

class IssueItemAdapter(private val items: ArrayList<OrderItem>): RecyclerView.Adapter<IssueItemAdapter.ViewHolder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(AdapterIssueItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items[position]
        val context = holder.itemView.context

        holder.binding.textTitle.text = item.typeMessageName
        holder.binding.textHint.text  = item.typeOrderName
    }

    var onClickListener: ((Int) -> Unit)? = null

    inner class ViewHolder(val binding: AdapterIssueItemBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onClickListener?.invoke(adapterPosition)
            }
        }
    }
}