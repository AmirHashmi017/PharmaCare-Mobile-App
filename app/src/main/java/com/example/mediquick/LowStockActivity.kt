package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LowStockActivity : AppCompatActivity() {

    private val actionChips = listOf("📋 View All", "✏️ Update Stock", "🔔 Alerts")
    private lateinit var db: AppDatabase
    private lateinit var adapter: MedicineAdapter
    private val lowStockList = mutableListOf<Medicine>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_low_stock)

        db = AppDatabase.getDatabase(this)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvLowStock)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = MedicineAdapter(
            lowStockList,
            onEdit        = { med ->
                val intent = Intent(this, AddEditMedicineActivity::class.java)
                intent.putExtra("medicine_id", med.id)
                startActivity(intent)
            },
            onDelete      = null,
            onAddCart     = null,
            editActionText = "Update"
        )
        rv.adapter = adapter

        lifecycleScope.launch {
            db.medicineDao().getLowStockMedicines().collectLatest { list ->
                lowStockList.clear()
                lowStockList.addAll(list)
                adapter.notifyDataSetChanged()

                findViewById<TextView>(R.id.tvAlertCount).text = "${list.size} low-stock items"

                if (list.isEmpty()) {
                    rv.visibility = View.GONE
                    findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
                } else {
                    rv.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tvEmpty).visibility = View.GONE
                }
            }
        }

        val gridActions = findViewById<GridView>(R.id.gridActions)
        gridActions.adapter = ChipAdapter(actionChips, selectedChip = "")
        gridActions.setOnItemClickListener { _, _, pos, _ ->
            when (pos) {
                0 -> Toast.makeText(this, "Showing all low-stock items below", Toast.LENGTH_SHORT).show()
                1 -> startActivity(Intent(this, InventoryActivity::class.java))
                2 -> Toast.makeText(this, "Alerts are active for items ≤ min stock", Toast.LENGTH_SHORT).show()
            }
        }
    }
}