package com.example.mediquick

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReceiptItemAdapter(private val items: List<SaleItem>) :
    RecyclerView.Adapter<ReceiptItemAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName    : TextView = v.findViewById(R.id.tvRiName)
        val tvQty     : TextView = v.findViewById(R.id.tvRiQty)
        val tvSubtotal: TextView = v.findViewById(R.id.tvRiSubtotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_receipt_row, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        val fmt = java.text.DecimalFormat("#,##0.00")
        h.tvName.text     = item.medicineName
        h.tvQty.text      = "x${item.quantity} @ Rs ${fmt.format(item.priceEach)}"
        h.tvSubtotal.text = "Rs ${fmt.format(item.subtotal)}"
    }
}
