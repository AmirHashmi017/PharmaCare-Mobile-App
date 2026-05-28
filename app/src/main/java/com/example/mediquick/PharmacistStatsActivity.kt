package com.example.mediquick

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class PharmacistStatsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: PharmacistStatsAdapter
    private val statsList = mutableListOf<PharmacistStat>()
    private var selectedFilter = "Today"
    
    // API Place 2: Using our custom Internal API for performance metrics
    private lateinit var internalApi: MediQuickApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacist_stats)

        db = AppDatabase.getDatabase(this)
        internalApi = MediQuickApi(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvPharmacists)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = PharmacistStatsAdapter(statsList)
        rv.adapter = adapter

        val spinner = findViewById<Spinner>(R.id.spinnerTimeFilter)
        val filters = arrayOf("Today", "Last Week", "Last Month")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filters)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFilter = filters[position]
                loadStats()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadStats()
    }

    private fun loadStats() {
        val startDate = getStartDate(selectedFilter)
        lifecycleScope.launch {
            val pharmacists = db.userDao().getAllPharmacists()
            val newList = mutableListOf<PharmacistStat>()

            for (pharmacist in pharmacists) {
                // Calling our internal API to get calculated report
                val report = internalApi.getPharmacistStats(pharmacist.uid, startDate)

                newList.add(PharmacistStat(
                    pharmacist = pharmacist,
                    totalOrders = report.totalOrders,
                    completedOrders = report.completedOrders,
                    pendingOrders = 0, // Simplified
                    revenue = report.revenue,
                    efficiencyScore = report.efficiency
                ))
            }
            newList.sortByDescending { it.revenue }

            runOnUiThread {
                statsList.clear()
                statsList.addAll(newList)
                adapter.notifyDataSetChanged()

                val totalRevenue = newList.sumOf { it.revenue }
                val totalOrders = newList.sumOf { it.totalOrders }
                val fmt = DecimalFormat("#,##0.00")

                findViewById<TextView>(R.id.tvTotalRevenue).text = "Rs ${fmt.format(totalRevenue)}"
                findViewById<TextView>(R.id.tvTotalOrders).text = "$totalOrders orders"

                val empty = findViewById<TextView>(R.id.tvEmpty)
                empty.visibility = if (newList.isEmpty()) View.VISIBLE else View.GONE
                findViewById<RecyclerView>(R.id.rvPharmacists).visibility =
                    if (newList.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun getStartDate(filter: String): String {
        val cal = Calendar.getInstance()
        when (filter) {
            "Today" -> {}
            "Last Week" -> cal.add(Calendar.DAY_OF_YEAR, -7)
            "Last Month" -> cal.add(Calendar.MONTH, -1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
}

data class PharmacistStat(
    val pharmacist: User,
    val totalOrders: Int,
    val completedOrders: Int,
    val pendingOrders: Int,
    val revenue: Double,
    val efficiencyScore: Float = 0f
)

class PharmacistStatsAdapter(
    private val items: List<PharmacistStat>
) : RecyclerView.Adapter<PharmacistStatsAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvInitial: TextView = v.findViewById(R.id.tvPharmacistInitial)
        val tvName: TextView = v.findViewById(R.id.tvPharmacistName)
        val tvEmail: TextView = v.findViewById(R.id.tvPharmacistEmail)
        val tvRevenue: TextView = v.findViewById(R.id.tvPharmacistRevenue)
        val tvTotalOrders: TextView = v.findViewById(R.id.tvPharmacistTotalOrders)
        val tvCompleted: TextView = v.findViewById(R.id.tvPharmacistCompleted)
        val tvPending: TextView = v.findViewById(R.id.tvPharmacistPending)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_pharmacist_stat, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val stat = items[pos]
        val fmt = DecimalFormat("#,##0.00")
        val p = stat.pharmacist

        h.tvInitial.text = p.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P"
        h.tvName.text = if (stat.efficiencyScore > 0) "${p.name} (⚡${stat.efficiencyScore.toInt()}%)" else p.name
        h.tvEmail.text = p.email
        h.tvRevenue.text = "Rs ${fmt.format(stat.revenue)}"
        h.tvTotalOrders.text = "${stat.totalOrders} orders"
        h.tvCompleted.text = "Efficiency: ${stat.efficiencyScore.toInt()}%"
        h.tvPending.text = ""
    }
}