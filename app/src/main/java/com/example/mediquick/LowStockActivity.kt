package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LowStockActivity : AppCompatActivity() {

    // Static action-hint labels shown in the GridView header
    private val actionChips = listOf("📋 View All", "✏️ Update Stock", "🔔 Alerts")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_low_stock)

        val db = DatabaseHelper(this)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val list = db.getLowStockMedicines()
        findViewById<TextView>(R.id.tvAlertCount).text = "${list.size} low-stock items"

        // ── GridView: static action chips ─────────────────────────────────────
        val gridActions = findViewById<GridView>(R.id.gridActions)
        gridActions.adapter = ChipAdapter(actionChips, selectedChip = "")
        gridActions.setOnItemClickListener { _, _, pos, _ ->
            when (pos) {
                0 -> Toast.makeText(this, "Showing all low-stock items below", Toast.LENGTH_SHORT).show()
                1 -> {
                    // Navigate to inventory to update stock
                    startActivity(Intent(this, InventoryActivity::class.java))
                }
                2 -> Toast.makeText(this, "Alerts are active for items ≤ min stock", Toast.LENGTH_SHORT).show()
            }
        }

        // ── RecyclerView: low-stock medicine list ─────────────────────────────
        val rv = findViewById<RecyclerView>(R.id.rvLowStock)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = MedicineAdapter(
            list,
            onEdit        = { med -> startActivity(Intent(this, AddEditMedicineActivity::class.java).putExtra("medicine_id", med.id)) },
            onDelete      = null,
            onAddCart     = null,
            editActionText = getString(R.string.update)
        )

        if (list.isEmpty()) {
            rv.visibility = View.GONE
            findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
        }
    }
}