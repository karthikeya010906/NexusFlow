package com.nexusflow.ui.create

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nexusflow.core.model.ConditionType
import com.nexusflow.databinding.ItemActionSelectedBinding

class SelectedConditionsAdapter(
    private val onRemove: (Int) -> Unit
) : ListAdapter<Pair<ConditionType, Map<String, String>>, SelectedConditionsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemActionSelectedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemActionSelectedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<ConditionType, Map<String, String>>, position: Int) {
            val (type, params) = item
            binding.actionName.text = "${type.displayName} (${params.values.joinToString()})"
            binding.actionIcon.setImageResource(android.R.drawable.ic_menu_info_details)
            binding.removeActionBtn.setOnClickListener { onRemove(position) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Pair<ConditionType, Map<String, String>>>() {
        override fun areItemsTheSame(
            oldItem: Pair<ConditionType, Map<String, String>>,
            newItem: Pair<ConditionType, Map<String, String>>
        ): Boolean = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: Pair<ConditionType, Map<String, String>>,
            newItem: Pair<ConditionType, Map<String, String>>
        ): Boolean = oldItem == newItem
    }
}