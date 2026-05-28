package com.example.mediquick

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SalesHistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var saleRepository: SaleRepository
    private var currentUser: User? = null
    private lateinit var adapter: SalesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_history)

        db = AppDatabase.getDatabase(this)
        auth = FirebaseAuth.getInstance()
        saleRepository = SaleRepository(db.saleDao())

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val rv = findViewById<RecyclerView>(R.id.rvSales)
        rv.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val uid = auth.currentUser?.uid ?: "ADMIN_ID"
            currentUser = db.userDao().getUserById(uid)

            val user = currentUser ?: return@launch

            if (user.role == UserRole.USER) {
                tvTitle?.text = "My Purchase History"
                findViewById<View>(R.id.cardRevenue)?.visibility = View.GONE
            } else {
                tvTitle?.text = "Sales History"
                findViewById<View>(R.id.cardRevenue)?.visibility = View.VISIBLE
            }

            val salesFlow = when (user.role) {
                UserRole.ADMIN -> db.saleDao().getAllSales()
                UserRole.PHARMACIST -> db.saleDao().getAllSales()
                UserRole.USER -> db.saleDao().getSalesByCustomer(user.uid)
            }

            salesFlow.collectLatest { sales ->
                adapter = SalesAdapter(
                    sales, user,
                    onUpdateStatus = { sale, status -> updateStatus(sale, status) },
                    onCancel = { sale -> cancelOrder(sale) }
                )
                rv.adapter = adapter

                val fmt = java.text.DecimalFormat("#,##0.00")
                val total = sales.filter {
                    it.status == "COMPLETED" || it.status == "PAYMENT_DONE"
                }.sumOf { it.total }
                findViewById<TextView>(R.id.tvTotalRevenue).text  = "Rs ${fmt.format(total)}"
                findViewById<TextView>(R.id.tvTotalSalesCount).text = "${sales.size} orders"

                if (sales.isEmpty()) {
                    rv.visibility = View.GONE
                    findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
                } else {
                    rv.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tvEmpty).visibility = View.GONE
                }
            }
        }
    }

    private fun updateStatus(sale: Sale, status: String) {
        lifecycleScope.launch {
            // Repository updates both Room and Firestore
            saleRepository.updateStatus(sale.id, status, auth.currentUser?.uid)
            Toast.makeText(this@SalesHistoryActivity, "Status updated to $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelOrder(sale: Sale) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Order")
            .setMessage("Are you sure you want to cancel this order?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    saleRepository.cancelOrder(sale.id)
                    Toast.makeText(this@SalesHistoryActivity, "Order cancelled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}

class SalesAdapter(
    private val items: List<Sale>,
    private val currentUser: User,
    private val onUpdateStatus: (Sale, String) -> Unit,
    private val onCancel: (Sale) -> Unit
) : RecyclerView.Adapter<SalesAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvId: TextView = v.findViewById(R.id.tvSaleId)
        val tvDate: TextView = v.findViewById(R.id.tvSaleDate)
        val tvCustomer: TextView = v.findViewById(R.id.tvSaleCustomer)
        val tvTotal: TextView = v.findViewById(R.id.tvSaleTotal)
        val tvStatus: TextView = v.findViewById(R.id.tvSaleStatus)
        val btnAction: Button = v.findViewById(R.id.btnAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sale, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val sale = items[pos]
        val fmt = java.text.DecimalFormat("#,##0.00")
        h.tvId.text = "Order #${sale.id}"
        h.tvDate.text = sale.date
        h.tvCustomer.text = if (currentUser.role == UserRole.USER) "" else sale.customerName
        h.tvTotal.text = "Rs ${fmt.format(sale.total)}"
        h.tvStatus.text = "Status: ${sale.status}"

        if (currentUser.role == UserRole.USER) {
            if (sale.status == "PAYMENT_PENDING") {
                h.btnAction.visibility = View.VISIBLE
                h.btnAction.text = "Cancel Order"
                h.btnAction.setOnClickListener { onCancel(sale) }
            } else {
                h.btnAction.visibility = View.GONE
            }
        } else {
            h.btnAction.visibility = View.VISIBLE
            h.btnAction.text = "Update Status"
            h.btnAction.setOnClickListener {
                val statuses = arrayOf("PAYMENT_DONE", "IN_SHIPPING", "COMPLETED", "CANCELLED")
                AlertDialog.Builder(h.itemView.context)
                    .setTitle("Update Status")
                    .setItems(statuses) { _, i -> onUpdateStatus(sale, statuses[i]) }
                    .show()
            }
        }
    }
}