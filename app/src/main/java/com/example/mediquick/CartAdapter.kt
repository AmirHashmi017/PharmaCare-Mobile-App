package com.example.mediquick

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(
    private val items: List<CartItem>,
    private val onRemove: (Int) -> Unit,
    private val onQuantityChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvCartName)
        val tvSubtotal: TextView = v.findViewById(R.id.tvCartSubtotal)
        val tvQty: TextView = v.findViewById(R.id.tvCartQty)
        val btnMinus: ImageButton = v.findViewById(R.id.btnMinus)
        val btnPlus: ImageButton = v.findViewById(R.id.btnPlus)
        val btnRemove: ImageButton = v.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        val fmt = java.text.DecimalFormat("#,##0.00")
        
        h.tvName.text = item.medicine.name
        h.tvSubtotal.text = "Rs ${fmt.format(item.subtotal)}"
        h.tvQty.text = item.quantity.toString()

        h.btnPlus.setOnClickListener {
            if (item.quantity < item.medicine.stock) {
                item.quantity++
                notifyItemChanged(pos)
                onQuantityChanged()
            }
        }

        h.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                notifyItemChanged(pos)
                onQuantityChanged()
            }
        }

        h.btnRemove.setOnClickListener {
            onRemove(pos)
        }
    }
}
