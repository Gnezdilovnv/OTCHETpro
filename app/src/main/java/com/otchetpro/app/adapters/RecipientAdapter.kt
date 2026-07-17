package com.otchetpro.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.data.Recipient

class RecipientAdapter(
    private val items: List<Recipient>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<RecipientAdapter.ViewHolder>() {

    class ViewHolder(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_recipient_name)
        val tvEmail: TextView = v.findViewById(R.id.tv_recipient_email)
        val btnDelete: android.widget.Button = v.findViewById(R.id.btn_recipient_delete)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): ViewHolder {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_recipient, p, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        h.tvName.text = items[pos].name
        h.tvEmail.text = items[pos].email
        h.btnDelete.setOnClickListener { onDelete(pos) }
    }

    override fun getItemCount() = items.size
}
