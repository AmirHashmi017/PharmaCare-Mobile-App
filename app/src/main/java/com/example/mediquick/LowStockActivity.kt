package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LowStockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_low_stock)

        val db = DatabaseHelper(this)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val list = db.getLowStockMedicines()
        findViewById<TextView>(R.id.tvAlertCount).text = "${list.size} low-stock items"

        val rv = findViewById<RecyclerView>(R.id.rvLowStock)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = MedicineAdapter(list,
            onEdit   = { med -> startActivity(Intent(this, AddEditMedicineActivity::class.java).putExtra("medicine_id", med.id)) },
            onDelete = null,
            onAddCart= null
        )

        if (list.isEmpty()) {
            rv.visibility = View.GONE
            findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
        }
    }
}
