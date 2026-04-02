package com.example.mediquick

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MedicineAdapter(
    private val items: List<Medicine>,
    private val onEdit:    ((Medicine) -> Unit)?,
    private val onDelete:  ((Medicine) -> Unit)?,
    private val onAddCart: ((Medicine) -> Unit)?,
    private val editActionText: String = "Edit"
) : RecyclerView.Adapter<MedicineAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivImage    : ImageView   = v.findViewById(R.id.ivMedImage)
        val tvInitial  : TextView    = v.findViewById(R.id.tvMedInitial)
        val tvName     : TextView    = v.findViewById(R.id.tvMedName)
        val tvCategory : TextView    = v.findViewById(R.id.tvMedCategory)
        val tvPrice    : TextView    = v.findViewById(R.id.tvMedPrice)
        val tvStock    : TextView    = v.findViewById(R.id.tvMedStock)
        val tvExpiry   : TextView    = v.findViewById(R.id.tvMedExpiry)
        val tvStockBadge: TextView   = v.findViewById(R.id.tvStockBadge)
        val btnEdit    : TextView    = v.findViewById(R.id.btnEdit)
        val btnDelete  : TextView    = v.findViewById(R.id.btnDelete)
        val btnAddCart : ImageButton = v.findViewById(R.id.btnAddCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_medicine, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = items[pos]
        val fmt = java.text.DecimalFormat("#,##0.00")

        h.btnEdit.text = editActionText
        h.tvName.text     = m.name
        h.tvCategory.text = m.category
        h.tvPrice.text    = "Rs ${fmt.format(m.price)} / ${m.unit}"
        h.tvStock.text    = "${m.stock} ${m.unit}"
        h.tvExpiry.text   = "Exp: ${m.expiryDate}"

        val initial = m.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
        val uriStr = m.imageUri
        if (uriStr.isNullOrBlank()) {
            h.ivImage.visibility = View.GONE
            h.tvInitial.visibility = View.VISIBLE
            h.tvInitial.text = initial
        } else {
            try {
                h.ivImage.setImageURI(android.net.Uri.parse(uriStr))
                h.ivImage.visibility = View.VISIBLE
                h.tvInitial.visibility = View.GONE
            } catch (_: Exception) {
                h.ivImage.visibility = View.GONE
                h.tvInitial.visibility = View.VISIBLE
                h.tvInitial.text = initial
            }
        }


        val isLow = m.stock <= m.minStock
        h.tvStockBadge.visibility = if (isLow) View.VISIBLE else View.GONE
        val color = if (isLow) R.color.error else R.color.green_primary
        h.tvStock.setTextColor(ContextCompat.getColor(h.itemView.context, color))


        h.btnEdit.visibility    = if (onEdit    != null) View.VISIBLE else View.GONE
        h.btnDelete.visibility  = if (onDelete  != null) View.VISIBLE else View.GONE
        h.btnAddCart.visibility = if (onAddCart != null) View.VISIBLE else View.GONE

        h.btnEdit.setOnClickListener    { onEdit?.invoke(m) }
        h.btnDelete.setOnClickListener  { onDelete?.invoke(m) }
        h.btnAddCart.setOnClickListener { onAddCart?.invoke(m) }
    }
}
