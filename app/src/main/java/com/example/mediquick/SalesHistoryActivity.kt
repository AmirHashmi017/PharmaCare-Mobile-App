package com.example.mediquick

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SalesHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_history)

        val db = DatabaseHelper(this)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val sales = db.getAllSales()
        val fmt   = java.text.DecimalFormat("#,##0.00")
        val total = sales.sumOf { it.total }

        findViewById<TextView>(R.id.tvTotalRevenue).text  = "Rs ${fmt.format(total)}"
        findViewById<TextView>(R.id.tvTotalSalesCount).text = "${sales.size} transactions"

        val rv = findViewById<RecyclerView>(R.id.rvSales)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SalesAdapter(sales)

        if (sales.isEmpty()) {
            rv.visibility = View.GONE
            findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
        }
    }
}

class SalesAdapter(private val items: List<Sale>) :
    RecyclerView.Adapter<SalesAdapter.VH>() {

    private val expanded = mutableSetOf<Long>()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvId      : TextView    = v.findViewById(R.id.tvSaleId)
        val tvDate    : TextView    = v.findViewById(R.id.tvSaleDate)
        val tvCustomer: TextView    = v.findViewById(R.id.tvSaleCustomer)
        val tvTotal   : TextView    = v.findViewById(R.id.tvSaleTotal)
        val tvItems   : TextView    = v.findViewById(R.id.tvSaleItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sale, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val sale = items[pos]
        val fmt  = java.text.DecimalFormat("#,##0.00")
        h.tvId.text       = "Sale #${sale.id}"
        h.tvDate.text     = sale.date
        h.tvCustomer.text = sale.customerName
        h.tvTotal.text    = "Rs ${fmt.format(sale.total)}"

        val isExp = expanded.contains(sale.id)
        h.tvItems.visibility = if (isExp) View.VISIBLE else View.GONE
        if (isExp) {
            h.tvItems.text = sale.items.joinToString("\n") {
                "• ${it.medicineName} x${it.quantity} — Rs ${fmt.format(it.subtotal)}"
            }
        }

        h.itemView.setOnClickListener {
            if (isExp) expanded.remove(sale.id) else expanded.add(sale.id)
            notifyItemChanged(pos)
        }
    }
}
