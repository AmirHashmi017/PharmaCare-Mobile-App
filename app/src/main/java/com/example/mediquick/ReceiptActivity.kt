package com.example.mediquick

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReceiptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        val db     = DatabaseHelper(this)
        val saleId = intent.getLongExtra("sale_id", -1L)
        val sale   = db.getAllSales().find { it.id == saleId } ?: run { finish(); return }
        val pharmacy = UserPrefs.getAccount(this)?.getString("pharmacy") ?: "PharmaCare"
        val fmt = java.text.DecimalFormat("#,##0.00")

        findViewById<TextView>(R.id.tvReceiptPharmacy).text = pharmacy
        findViewById<TextView>(R.id.tvReceiptId).text       = "Receipt #${sale.id}"
        findViewById<TextView>(R.id.tvReceiptDate).text     = sale.date
        findViewById<TextView>(R.id.tvReceiptCustomer).text = "Customer: ${sale.customerName}"
        findViewById<TextView>(R.id.tvReceiptTotal).text    = "Rs ${fmt.format(sale.total)}"

        val rv = findViewById<RecyclerView>(R.id.rvReceiptItems)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ReceiptItemAdapter(sale.items)

        findViewById<TextView>(R.id.btnNewSale).setOnClickListener { finish() }
    }
}
