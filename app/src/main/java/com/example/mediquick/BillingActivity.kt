package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BillingActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var saleRepository: SaleRepository
    private lateinit var medicineRepository: MedicineRepository
    private val medicines = mutableListOf<Medicine>()
    private val cart = mutableListOf<CartItem>()
    private lateinit var medAdapter: MedicineAdapter
    private lateinit var cartAdapter: CartAdapter
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing)
        db = AppDatabase.getDatabase(this)
        saleRepository = SaleRepository(db.saleDao())
        medicineRepository = MedicineRepository(db.medicineDao())

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "ADMIN_ID"
            currentUser = db.userDao().getUserById(uid)
        }

        val rvMeds = findViewById<RecyclerView>(R.id.rvMedicines)
        medAdapter = MedicineAdapter(medicines, onEdit = null, onDelete = null, onAddCart = { med -> addToCart(med) })
        rvMeds.layoutManager = LinearLayoutManager(this)
        rvMeds.adapter = medAdapter

        val rvCart = findViewById<RecyclerView>(R.id.rvCart)
        cartAdapter = CartAdapter(
            cart,
            onRemove = { pos -> removeFromCart(pos) },
            onQuantityChanged = { updateTotal() }
        )
        rvCart.layoutManager = LinearLayoutManager(this)
        rvCart.adapter = cartAdapter

        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = loadMedicines()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        findViewById<Button>(R.id.btnCheckout).setOnClickListener { checkout() }

        loadMedicines()
    }

    private fun loadMedicines() {
        val q = findViewById<EditText>(R.id.etSearch).text.toString()
        lifecycleScope.launch {
            db.medicineDao().searchMedicinesWithCategory(q, "All").collectLatest { list ->
                medicines.clear()
                medicines.addAll(list)
                medAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun addToCart(med: Medicine) {
        if (med.stock == 0) {
            Toast.makeText(this, "${med.name} is out of stock!", Toast.LENGTH_SHORT).show()
            return
        }
        val existing = cart.find { it.medicine.id == med.id }
        if (existing != null) {
            if (existing.quantity >= med.stock) {
                Toast.makeText(this, "Not enough stock", Toast.LENGTH_SHORT).show(); return
            }
            existing.quantity++
            cartAdapter.notifyDataSetChanged()
        } else {
            cart.add(CartItem(med, 1))
            cartAdapter.notifyItemInserted(cart.size - 1)
        }
        updateTotal()
        Toast.makeText(this, "${med.name} added to cart", Toast.LENGTH_SHORT).show()
    }

    private fun removeFromCart(pos: Int) {
        cart.removeAt(pos)
        cartAdapter.notifyItemRemoved(pos)
        updateTotal()
    }

    fun updateTotal() {
        val total = cart.sumOf { it.subtotal }
        val fmt = java.text.DecimalFormat("#,##0.00")
        findViewById<TextView>(R.id.tvTotal).text = "Total: Rs ${fmt.format(total)}"
        findViewById<TextView>(R.id.tvCartCount).text = "${cart.size} item(s) in cart"
        findViewById<Button>(R.id.btnCheckout).isEnabled = cart.isNotEmpty()
    }

    private fun checkout() {
        if (cart.isEmpty()) return
        val total = cart.sumOf { it.subtotal }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

        val user = currentUser
        val uid = user?.uid ?: "GUEST"
        val customerName = findViewById<EditText>(R.id.etCustomer).text.toString().trim()
            .ifEmpty { user?.name ?: "Walk-in Customer" }

        val status = if (user?.role == UserRole.USER) "PAYMENT_PENDING" else "COMPLETED"

        lifecycleScope.launch {
            val sale = Sale(
                customerName = customerName,
                customerId = uid,
                date = sdf.format(java.util.Date()),
                total = total,
                status = status,
                pharmacistId = if (user?.role == UserRole.PHARMACIST) uid else null
            )

            // Uses SaleRepository — saves to both Room AND Firestore
            val saleId = saleRepository.insertSaleWithItems(sale, cart)

            // Update stock via repository so Firestore stock count stays in sync too
            for (item in cart) {
                val updatedMed = item.medicine.copy(stock = item.medicine.stock - item.quantity)
                medicineRepository.update(updatedMed)
            }

            runOnUiThread {
                Toast.makeText(this@BillingActivity, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this@BillingActivity, ReceiptActivity::class.java)
                        .putExtra("sale_id", saleId)
                )
                finish()
            }
        }
    }
}