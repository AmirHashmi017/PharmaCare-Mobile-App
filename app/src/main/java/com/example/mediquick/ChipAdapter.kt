package com.example.mediquick

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class ChipAdapter(
    private val items: List<String>,
    private val selectedChip: String
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_grid_chip, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = items[position]
        holder.text.text = item

        // Optional: highlight selected chip
        if (item == selectedChip) {
            holder.text.alpha = 1.0f
        } else {
            holder.text.alpha = 0.7f
        }

        return view
    }

    private class ViewHolder(view: View) {
        val text: TextView = view.findViewById(R.id.tvChip)
    }
}