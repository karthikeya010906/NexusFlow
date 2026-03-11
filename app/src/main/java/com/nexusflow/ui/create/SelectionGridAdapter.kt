package com.nexusflow.ui.create

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexusflow.databinding.ItemSelectionGridBinding

class SelectionGridAdapter<T>(
    private val items: List<T>,
    private val getName: (T) -> String,
    private val getIcon: (T) -> Int,
    private val onClick: (T) -> Unit
) : RecyclerView.Adapter<SelectionGridAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSelectionGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSelectionGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.name.text = getName(item)
        holder.binding.icon.setImageResource(getIcon(item))
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}