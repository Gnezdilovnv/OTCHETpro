package com.otchetpro.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.data.Report

class ReportAdapter(
    private val onItemClick: (Report) -> Unit,
    private val onLongClick: (Report) -> Unit = {}
) : ListAdapter<Report, ReportAdapter.ReportViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view, onItemClick, onLongClick)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReportViewHolder(
        itemView: View,
        private val onItemClick: (Report) -> Unit,
        private val onLongClick: (Report) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTemplate: TextView = itemView.findViewById(R.id.tv_template)
        private val tvPreview: TextView = itemView.findViewById(R.id.tv_preview)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)

        fun bind(report: Report) {
            tvTemplate.text = report.templateName
            tvPreview.text = if (report.text.length > 60) report.text.take(60) + "..." else report.text
            
            if (report.status == "sent") {
                tvStatus.text = "Отправлен ✓"
                tvStatus.setBackgroundColor(0xFFDDF0E6.toInt())
                tvStatus.setTextColor(0xFF0F6B3A.toInt())
            } else {
                tvStatus.text = "Сохранен"
                tvStatus.setBackgroundColor(0xFFEEF4FC.toInt())
                tvStatus.setTextColor(0xFF1A4CBA.toInt())
            }
            
            itemView.setOnClickListener { onItemClick(report) }
            itemView.setOnLongClickListener {
                onLongClick(report)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem == newItem
        }
    }
}
