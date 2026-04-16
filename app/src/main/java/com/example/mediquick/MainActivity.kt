package com.example.mediquick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    // ── System broadcast receiver (network + airplane mode) ──────────────────
    private val networkReceiver = NetworkReceiver()

    // ── Local receiver that listens to the app-local network-changed action ──
    private val localNetworkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NetworkReceiver.ACTION_NETWORK_CHANGED -> {
                    val connected = intent.getBooleanExtra(NetworkReceiver.EXTRA_IS_CONNECTED, false)
                    val type      = intent.getStringExtra(NetworkReceiver.EXTRA_TYPE) ?: "None"
                    val msg = if (connected) "📶 Connected via $type" else "⚠️ No internet connection"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    updateNetworkBanner(connected, type)
                }
                NetworkReceiver.ACTION_AIRPLANE_MODE_CHANGED -> {
                    val on = intent.getBooleanExtra(NetworkReceiver.EXTRA_AIRPLANE_ON, false)
                    val msg = if (on) "✈️ Airplane mode ON — offline only" else "✈️ Airplane mode OFF"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    updateNetworkBanner(!on, if (on) "Airplane Mode" else "Reconnecting…")
                }
            }
        }
    }

    // ── Local receiver for auth events ────────────────────────────────────────
    private val authReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val name = intent.getStringExtra(AuthBroadcast.EXTRA_USER_NAME) ?: return
            when (intent.action) {
                AuthBroadcast.ACTION_SIGNED_IN  -> Toast.makeText(context, "✅ Signed in as $name", Toast.LENGTH_SHORT).show()
                AuthBroadcast.ACTION_SIGNED_UP  -> Toast.makeText(context, "🎉 Account created for $name", Toast.LENGTH_SHORT).show()
                AuthBroadcast.ACTION_SIGNED_OUT -> Toast.makeText(context, "👋 Signed out", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = DatabaseHelper(this)

        val account      = UserPrefs.getAccount(this)
        val pharmacyName = account?.getString("pharmacy") ?: "My Pharmacy"
        val userName     = account?.getString("name") ?: "Pharmacist"

        findViewById<TextView>(R.id.tvPharmacyName).text = pharmacyName
        findViewById<TextView>(R.id.tvUserName).text =
            getString(R.string.dashboard_greeting, userName)

        refreshStats()

        // ── Nav cards ────────────────────────────────────────────────────────
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

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startActivity(Intent(this, AddEditMedicineActivity::class.java))
        }

        // ── Logout ───────────────────────────────────────────────────────────
        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            UserPrefs.setLoggedIn(this, false)
            // Local broadcast: signed-out event
            sendBroadcast(Intent(AuthBroadcast.ACTION_SIGNED_OUT).setPackage(packageName))
            startActivity(Intent(this, LandingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onStart() {
        super.onStart()

        // ── Register system broadcast for network + airplane ─────────────────
        val sysFilter = IntentFilter().apply {
            @Suppress("DEPRECATION")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        registerReceiver(networkReceiver, sysFilter)

        // ── Register local receiver for the app-internal rebroadcast ─────────
        val localNetFilter = IntentFilter().apply {
            addAction(NetworkReceiver.ACTION_NETWORK_CHANGED)
            addAction(NetworkReceiver.ACTION_AIRPLANE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this, localNetworkReceiver, localNetFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // ── Register local receiver for auth events ───────────────────────────
        val authFilter = IntentFilter().apply {
            addAction(AuthBroadcast.ACTION_SIGNED_IN)
            addAction(AuthBroadcast.ACTION_SIGNED_UP)
            addAction(AuthBroadcast.ACTION_SIGNED_OUT)
        }
        ContextCompat.registerReceiver(
            this, authReceiver, authFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val status = NetworkReceiver.getNetworkStatus(this)
        updateNetworkBanner(status.isConnected, status.type)
    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
        unregisterReceiver(localNetworkReceiver)
        unregisterReceiver(authReceiver)
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        val fmt = java.text.DecimalFormat("#,##0.00")
        findViewById<TextView>(R.id.tvStatMedicines).text   = db.getTotalMedicines().toString()
        findViewById<TextView>(R.id.tvStatLowStock).text    = db.getLowStockCount().toString()
        findViewById<TextView>(R.id.tvStatTodaySales).text  = "Rs ${fmt.format(db.getTodaySalesTotal())}"
        findViewById<TextView>(R.id.tvStatTotalSales).text  = db.getTotalSales().toString()
    }

    /** Shows/hides the network status banner at the top of the dashboard. */
    private fun updateNetworkBanner(connected: Boolean, type: String) {
        val banner  = findViewById<View>(R.id.networkBanner)   ?: return
        val tvBanner = findViewById<TextView>(R.id.tvNetworkBanner) ?: return
        if (!connected) {
            tvBanner.text = "⚠️  No internet — running offline  ($type)"
            banner.visibility = View.VISIBLE
        } else {
            banner.visibility = View.GONE
        }
    }
}