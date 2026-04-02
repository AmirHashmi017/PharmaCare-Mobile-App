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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BillingActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private val medicines = mutableListOf<Medicine>()
    private val cart = mutableListOf<CartItem>()
    private lateinit var medAdapter: MedicineAdapter
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing)
        db = DatabaseHelper(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

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
        val list = db.getAllMedicines(q)
        medicines.clear(); medicines.addAll(list)
        medAdapter.notifyDataSetChanged()
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
        val customer = findViewById<EditText>(R.id.etCustomer).text.toString().trim()
            .ifEmpty { "Walk-in Customer" }
        val sale = Sale(customerName = customer, date = sdf.format(java.util.Date()), total = total)
        val saleId = db.insertSale(sale, cart)

        startActivity(
            Intent(this, ReceiptActivity::class.java)
                .putExtra("sale_id", saleId)
        )
        cart.clear()
        cartAdapter.notifyDataSetChanged()
        updateTotal()
        loadMedicines()
    }
}


class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onRemove: (Int) -> Unit,
    private val onQuantityChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName    : TextView    = v.findViewById(R.id.tvCartName)
        val tvQty     : TextView    = v.findViewById(R.id.tvCartQty)
        val tvSubtotal: TextView    = v.findViewById(R.id.tvCartSubtotal)
        val btnMinus  : ImageButton = v.findViewById(R.id.btnMinus)
        val btnPlus   : ImageButton = v.findViewById(R.id.btnPlus)
        val btnRemove : ImageButton = v.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        val fmt = java.text.DecimalFormat("#,##0.00")
        h.tvName.text     = item.medicine.name
        h.tvQty.text      = item.quantity.toString()
        h.tvSubtotal.text = "Rs ${fmt.format(item.subtotal)}"

        h.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                notifyItemChanged(pos)
                onQuantityChanged()
            } else {
                onRemove(pos)
            }
        }
        h.btnPlus.setOnClickListener {
            if (item.quantity < item.medicine.stock) {
                item.quantity++
                notifyItemChanged(pos)
                onQuantityChanged()
            } else {
                Toast.makeText(h.itemView.context, "Max stock reached", Toast.LENGTH_SHORT).show()
            }
        }
        h.btnRemove.setOnClickListener { onRemove(pos) }
    }
}