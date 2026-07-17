package com.otchetpro.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.data.Template

class TemplateAdapter(
    private val items: List<Template>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>() {

    class ViewHolder(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_template_name)
        val tvPreview: TextView = v.findViewById(R.id.tv_template_preview)
        val btnEdit: android.widget.Button = v.findViewById(R.id.btn_template_edit)
        val btnDelete: android.widget.Button = v.findViewById(R.id.btn_template_delete)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): ViewHolder {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_template, p, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        h.tvName.text = items[pos].name
        h.tvPreview.text = items[pos].text.take(60) + if (items[pos].text.length > 60) "..." else ""
        h.btnEdit.setOnClickListener { onEdit(pos) }
        h.btnDelete.setOnClickListener { onDelete(pos) }
    }

    override fun getItemCount() = items.size
}
