package com.otchetpro.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R

class SubDeptAdapter(
    private val items: List<String>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<SubDeptAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_subdept_name)
        val btnDelete: android.widget.Button = v.findViewById(R.id.btn_subdept_delete)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): ViewHolder {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_subdept, p, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        h.tvName.text = items[pos]
        h.btnDelete.setOnClickListener { onDelete(pos) }
    }

    override fun getItemCount() = items.size
}
