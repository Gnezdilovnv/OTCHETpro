package com.otchetpro.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.data.Variable
import com.otchetpro.app.data.VariableTypes

class VariableAdapter(
    private val items: List<Variable>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<VariableAdapter.ViewHolder>() {

    class ViewHolder(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_var_name)
        val tvType: TextView = v.findViewById(R.id.tv_var_type)
        val btnDelete: android.widget.Button = v.findViewById(R.id.btn_var_delete)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): ViewHolder {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_variable, p, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val item = items[pos]
        h.tvName.text = item.name + if (item.required) " *" else ""
        h.tvType.text = VariableTypes.displayNames[item.type] ?: item.type
        h.btnDelete.setOnClickListener { onDelete(pos) }
    }

    override fun getItemCount() = items.size
}
