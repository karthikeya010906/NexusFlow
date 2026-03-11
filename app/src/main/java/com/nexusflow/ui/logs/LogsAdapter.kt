package com.nexusflow.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nexusflow.data.db.entities.LogEntry
import com.nexusflow.databinding.ItemLogEntryBinding
import java.text.SimpleDateFormat
import java.util.*

class LogsAdapter : ListAdapter<LogEntry, LogsAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLogEntryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemLogEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: LogEntry) {
            binding.logTimestamp.text = dateFormat.format(Date(log.timestamp))
            binding.logRuleName.text = log.ruleName
            binding.logActionSummary.text = log.actionSummary
            
            binding.logStatusIcon.text = if (log.success) "✓" else "✗"
            binding.logStatusIcon.setTextColor(if (log.success) 0xFF4ADE80.toInt() else 0xFFF87171.toInt())
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}