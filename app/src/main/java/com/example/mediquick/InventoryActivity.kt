package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class InventoryActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var adapter: MedicineAdapter
    private val medicines = mutableListOf<Medicine>()
    private var selectedCategory = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        db = DatabaseHelper(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvMedicines)
        adapter = MedicineAdapter(medicines,
            onEdit   = { med -> startActivity(Intent(this, AddEditMedicineActivity::class.java).putExtra("medicine_id", med.id)) },
            onDelete = { med -> confirmDelete(med) },
            onAddCart= null,
            editActionText = getString(R.string.edit)
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter


        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = loadData()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })


        setupCategorySpinner()


        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startActivity(Intent(this, AddEditMedicineActivity::class.java))
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        setupCategorySpinner()
        loadData()
    }

    private fun setupCategorySpinner() {
        val cats = db.getCategories()
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cats)
        val idx = cats.indexOf(selectedCategory).coerceAtLeast(0)
        spinner.setSelection(idx)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedCategory = cats[pos]; loadData()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun loadData() {
        val q = findViewById<EditText>(R.id.etSearch).text.toString()
        val list = db.getAllMedicines(q, selectedCategory)
        medicines.clear(); medicines.addAll(list)
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.tvCount).text = "${list.size} medicines"
        findViewById<TextView>(R.id.tvEmpty).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(med: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("Delete Medicine")
            .setMessage("Delete '${med.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> db.deleteMedicine(med.id); loadData() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
