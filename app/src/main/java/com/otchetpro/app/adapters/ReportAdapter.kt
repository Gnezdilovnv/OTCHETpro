package com.otchetpro.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.data.Report

class ReportAdapter(
    private val onClick: (Report) -> Unit
) : ListAdapter<Report, ReportAdapter.ViewHolder>(Diff()) {

    class ViewHolder(item: android.view.View) : RecyclerView.ViewHolder(item) {
        private val tvTemplate: TextView = item.findViewById(R.id.tv_template)
        private val tvPreview: TextView = item.findViewById(R.id.tv_preview)
        private val tvStatus: TextView = item.findViewById(R.id.tv_status)

        fun bind(r: Report) {
            tvTemplate.text = r.templateName
            tvPreview.text = if (r.text.length > 60) r.text.take(60) + "..." else r.text
            tvStatus.text = if (r.status == "sent") "Отправлен ✓" else "Сохранен"
            tvStatus.setBackgroundColor(if (r.status == "sent") 0xFFDDF0E6.toInt() else 0xFFEEF4FC.toInt())
            tvStatus.setTextColor(if (r.status == "sent") 0xFF0F6B3A.toInt() else 0xFF1A4CBA.toInt())
            itemView.setOnClickListener { onClick(r) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): ViewHolder {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_report, p, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(h: ViewHolder, p: Int) { h.bind(getItem(p)) }

    class Diff : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(a: Report, b: Report) = a.id == b.id
        override fun areContentsTheSame(a: Report, b: Report) = a == b
    }
}
