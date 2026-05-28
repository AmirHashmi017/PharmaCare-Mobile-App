package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var saleRepository: SaleRepository
    private var currentUser: User? = null
    private var selectedTimeFilter = "Today"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)
        auth = FirebaseAuth.getInstance()
        medicineRepository = MedicineRepository(db.medicineDao())
        saleRepository = SaleRepository(db.saleDao())

        loadUserAndSync()

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val user = currentUser
            if (user?.role == UserRole.PHARMACIST) {
                startActivity(Intent(this, AddEditMedicineActivity::class.java))
            }
        }

        findViewById<View>(R.id.cardAddPharmacist).setOnClickListener {
            startActivity(Intent(this, AddPharmacistActivity::class.java))
        }
        findViewById<View>(R.id.cardPharmacistStats).setOnClickListener {
            startActivity(Intent(this, PharmacistStatsActivity::class.java))
        }

        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            auth.signOut()
            lifecycleScope.launch {
                db.userDao().clearUsers()
                UserPrefs.setLoggedIn(this@MainActivity, false)
                startActivity(Intent(this@MainActivity, LandingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
        }

        val spinnerFilter = findViewById<Spinner>(R.id.spinnerTimeFilter)
        val filters = arrayOf("Today", "Last Week", "Last Month")
        spinnerFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filters)
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTimeFilter = filters[position]
                refreshStats()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        setupNavigation()
    }

    private fun loadUserAndSync() {
        lifecycleScope.launch {
            Log.d("MediQuick", "=== loadUserAndSync START ===")

            // Step 1: resolve user
            var user: User? = db.userDao().getUserById("ADMIN_ID")
            if (user == null) {
                val uid = auth.currentUser?.uid
                Log.d("MediQuick", "Firebase UID = $uid")
                if (uid != null) user = db.userDao().getUserById(uid)
            }

            if (user == null) {
                Log.w("MediQuick", "No user found — redirecting to SignIn")
                startActivity(Intent(this@MainActivity, SignInActivity::class.java))
                finish()
                return@launch
            }

            Log.d("MediQuick", "User loaded: ${user.name} role=${user.role} uid=${user.uid}")
            currentUser = user
            updateUI()

            // Step 2: DEBUG — check Firestore directly before sync
            debugFirestoreDirectly()

            // Step 3: sync
            Log.d("MediQuick", "--- Starting Firestore sync ---")
            try {
                medicineRepository.syncFromFirestore()
            } catch (e: Exception) {
                Log.e("MediQuick", "Medicine sync exception: ${e.message}", e)
            }
            try {
                saleRepository.syncFromFirestore(user.uid, user.role)
            } catch (e: Exception) {
                Log.e("MediQuick", "Sale sync exception: ${e.message}", e)
            }
            Log.d("MediQuick", "--- Sync done ---")

            // Step 4: refresh stats after sync
            refreshStats()
        }
    }

    /**
     * Raw Firestore read — bypasses all repository logic to confirm
     * what is actually in the 'medicines' collection right now.
     */
    private suspend fun debugFirestoreDirectly() {
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Check medicines
            val medSnap = firestore.collection("medicines").get().await()
            Log.d("MediQuick", "🔍 DEBUG: 'medicines' collection has ${medSnap.size()} docs")
            for (doc in medSnap.documents) {
                Log.d("MediQuick", "  medicine doc: id=${doc.id} name=${doc.getString("name")}")
            }

            // Check sales
            val saleSnap = firestore.collection("sales").get().await()
            Log.d("MediQuick", "🔍 DEBUG: 'sales' collection has ${saleSnap.size()} docs")
            for (doc in saleSnap.documents) {
                Log.d("MediQuick", "  sale doc: id=${doc.id} customer=${doc.getString("customerName")} status=${doc.getString("status")}")
            }

        } catch (e: Exception) {
            Log.e("MediQuick", "🔍 DEBUG Firestore direct read FAILED: ${e.message}", e)
        }
    }

    private fun updateUI() {
        val user = currentUser ?: return
        findViewById<TextView>(R.id.tvPharmacyName).text = "MediQuick"

        val roleDisplay = when (user.role) {
            UserRole.ADMIN      -> "System Administrator"
            UserRole.PHARMACIST -> "Pharmacist"
            UserRole.USER       -> "Customer"
        }
        findViewById<TextView>(R.id.tvUserName).text = "$roleDisplay: ${user.name}"

        val cardInventory       = findViewById<View>(R.id.cardInventory)
        val cardBilling         = findViewById<View>(R.id.cardBilling)
        val cardHistory         = findViewById<View>(R.id.cardHistory)
        val cardAlerts          = findViewById<View>(R.id.cardAlerts)
        val cardAddPharmacist   = findViewById<View>(R.id.cardAddPharmacist)
        val cardPharmacistStats = findViewById<View>(R.id.cardPharmacistStats)
        val fab                 = findViewById<FloatingActionButton>(R.id.fab)
        val tvHistoryTitle      = findViewById<TextView>(R.id.tvHistoryTitle)
        val tvBillingTitle      = findViewById<TextView>(R.id.tvBillingTitle)

        cardInventory.visibility       = View.VISIBLE
        cardBilling.visibility         = View.VISIBLE
        cardHistory.visibility         = View.VISIBLE
        cardAlerts.visibility          = View.VISIBLE
        cardAddPharmacist.visibility   = View.GONE
        cardPharmacistStats.visibility = View.GONE
        findViewById<View>(R.id.statCardRevenue).visibility  = View.VISIBLE
        findViewById<View>(R.id.statCardLowStock).visibility = View.VISIBLE
        findViewById<View>(R.id.filterLayout).visibility    = View.VISIBLE

        when (user.role) {
            UserRole.ADMIN -> {
                cardBilling.visibility         = View.GONE
                cardAddPharmacist.visibility   = View.VISIBLE
                fab.visibility                 = View.GONE
                cardPharmacistStats.visibility = View.VISIBLE
                tvHistoryTitle.text            = "Sales History"
            }
            UserRole.PHARMACIST -> {
                cardAddPharmacist.visibility   = View.GONE
                fab.visibility                 = View.VISIBLE
                cardPharmacistStats.visibility = View.GONE
                tvHistoryTitle.text            = "Sales History"
                tvBillingTitle.text            = "Create Sale"
            }
            UserRole.USER -> {
                cardInventory.visibility       = View.GONE
                cardAlerts.visibility          = View.GONE
                cardPharmacistStats.visibility = View.GONE
                fab.visibility                 = View.GONE
                tvHistoryTitle.text            = "My Purchase History"
                tvBillingTitle.text            = "Order Medicines"
                findViewById<View>(R.id.statCardRevenue).visibility  = View.GONE
                findViewById<View>(R.id.statCardLowStock).visibility = View.GONE
                findViewById<View>(R.id.filterLayout).visibility    = View.GONE
            }
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.cardInventory).setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }
        findViewById<View>(R.id.cardBilling).setOnClickListener {
            startActivity(Intent(this, BillingActivity::class.java))
        }
        findViewById<View>(R.id.cardHistory).setOnClickListener {
            startActivity(Intent(this, SalesHistoryActivity::class.java))
        }
        findViewById<View>(R.id.cardAlerts).setOnClickListener {
            startActivity(Intent(this, LowStockActivity::class.java))
        }
    }

    private fun refreshStats() {
        val user = currentUser ?: return
        lifecycleScope.launch {
            val totalMeds     = db.medicineDao().getCount()
            val lowStockItems = db.medicineDao().getLowStockMedicines().first()

            Log.d("MediQuick", "Stats: totalMeds=$totalMeds lowStock=${lowStockItems.size}")

            findViewById<TextView>(R.id.tvStatMedicines).text = totalMeds.toString()
            findViewById<TextView>(R.id.tvStatLowStock).text  = lowStockItems.size.toString()

            val statsVal    = findViewById<TextView>(R.id.tvStatTotalSales)
            val statsLabel  = findViewById<TextView>(R.id.tvStatTotalSalesLabel)
            val revenueVal  = findViewById<TextView>(R.id.tvStatTodaySales)
            val startDate   = getStartDate(selectedTimeFilter)

            val salesFlow = when (user.role) {
                UserRole.ADMIN      -> db.saleDao().getSalesFromDate(startDate)
                UserRole.PHARMACIST -> db.saleDao().getSalesFromDateByPharmacist(user.uid, startDate)
                UserRole.USER       -> db.saleDao().getSalesFromDateByCustomer(user.uid, startDate)
            }

            val sales = salesFlow.first()
            Log.d("MediQuick", "Stats: sales count=${sales.size}")

            statsVal.text   = sales.size.toString()
            statsLabel.text = if (user.role == UserRole.USER) "My Orders" else "Total Orders"

            val completedRevenue = sales.filter {
                it.status == "COMPLETED" || it.status == "PAYMENT_DONE" || it.status == "IN_SHIPPING"
            }.sumOf { it.total }

            val fmt = java.text.DecimalFormat("#,##0.00")
            revenueVal.text = "Rs ${fmt.format(completedRevenue)}"
        }
    }

    private fun getStartDate(filter: String): String {
        val cal = Calendar.getInstance()
        when (filter) {
            "Today"      -> {}
            "Last Week"  -> cal.add(Calendar.DAY_OF_YEAR, -7)
            "Last Month" -> cal.add(Calendar.MONTH, -1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }
}