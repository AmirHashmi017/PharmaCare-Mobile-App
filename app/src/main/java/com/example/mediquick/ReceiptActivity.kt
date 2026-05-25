package com.example.mediquick

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ReceiptActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        db = AppDatabase.getDatabase(this)
        val saleId = intent.getLongExtra("sale_id", -1L)
        
        if (saleId == -1L) {
            finish()
            return
        }

        val fmt = java.text.DecimalFormat("#,##0.00")

        lifecycleScope.launch {
            val sale = db.saleDao().getSaleById(saleId)
            if (sale == null) {
                finish()
                return@launch
            }
            
            val items = db.saleDao().getSaleItems(saleId)
            sale.items = items

            runOnUiThread {
                findViewById<TextView>(R.id.tvReceiptPharmacy).text = "MediQuick"
                findViewById<TextView>(R.id.tvReceiptId).text       = "Receipt #${sale.id}"
                findViewById<TextView>(R.id.tvReceiptDate).text     = sale.date
                findViewById<TextView>(R.id.tvReceiptCustomer).text = "Customer: ${sale.customerName}"
                findViewById<TextView>(R.id.tvReceiptTotal).text    = "Rs ${fmt.format(sale.total)}"

                val rv = findViewById<RecyclerView>(R.id.rvReceiptItems)
                rv.layoutManager = LinearLayoutManager(this@ReceiptActivity)
                rv.adapter = ReceiptItemAdapter(sale.items)
            }
        }

        findViewById<TextView>(R.id.btnNewSale).setOnClickListener { finish() }
    }
}
