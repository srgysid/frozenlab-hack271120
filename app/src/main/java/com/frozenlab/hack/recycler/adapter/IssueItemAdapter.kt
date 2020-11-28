package com.frozenlab.hack.recycler.adapter

import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.frozenlab.hack.api.models.IssueItem
import com.frozenlab.hack.databinding.AdapterIssueItemBinding

class IssueItemAdapter(private val items: ArrayList<IssueItem>): RecyclerView.Adapter<IssueItemAdapter.ViewHolder>() {

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

        holder.binding.textTitle.text = item.title
        holder.binding.textHint.text  = item.description
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