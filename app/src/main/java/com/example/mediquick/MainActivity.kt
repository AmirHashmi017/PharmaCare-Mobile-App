package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = DatabaseHelper(this)

        val account = UserPrefs.getAccount(this)
        val pharmacyName = account?.getString("pharmacy") ?: "My Pharmacy"
        val userName = account?.getString("name") ?: "Pharmacist"

        findViewById<TextView>(R.id.tvPharmacyName).text = pharmacyName
        findViewById<TextView>(R.id.tvUserName).text = "Hi, $userName 👋"

        refreshStats()


        findViewById<androidx.cardview.widget.CardView>(R.id.cardInventory).setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardBilling).setOnClickListener {
            startActivity(Intent(this, BillingActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardHistory).setOnClickListener {
            startActivity(Intent(this, SalesHistoryActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardAlerts).setOnClickListener {
            startActivity(Intent(this, LowStockActivity::class.java))
        }


        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startActivity(Intent(this, AddEditMedicineActivity::class.java))
        }


        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            UserPrefs.setLoggedIn(this, false)
            startActivity(Intent(this, LandingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        val fmt = java.text.DecimalFormat("#,##0.00")
        findViewById<TextView>(R.id.tvStatMedicines).text = db.getTotalMedicines().toString()
        findViewById<TextView>(R.id.tvStatLowStock).text  = db.getLowStockCount().toString()
        findViewById<TextView>(R.id.tvStatTodaySales).text = "Rs ${fmt.format(db.getTodaySalesTotal())}"
        findViewById<TextView>(R.id.tvStatTotalSales).text = db.getTotalSales().toString()
    }
}
