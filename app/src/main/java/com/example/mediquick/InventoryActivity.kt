package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InventoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var adapter: MedicineAdapter
    private val medicines = mutableListOf<Medicine>()
    private var selectedCategory = "All"
    private val categories = mutableListOf("All")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        db = AppDatabase.getDatabase(this)
        medicineRepository = MedicineRepository(db.medicineDao())

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvMedicines)
        adapter = MedicineAdapter(medicines,
            onEdit   = { med ->
                val intent = Intent(this, AddEditMedicineActivity::class.java)
                intent.putExtra("medicine_id", med.id)
                startActivity(intent)
            },
            onDelete = { med -> confirmDelete(med) },
            onAddCart= null,
            editActionText = "Edit"
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

    private fun setupCategorySpinner() {
        categories.clear()
        categories.add("All")
        categories.addAll(MedicineConstants.CATEGORIES)

        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedCategory = categories[pos]
                loadData()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun loadData() {
        val query = findViewById<EditText>(R.id.etSearch).text.toString().trim()
        lifecycleScope.launch {
            db.medicineDao().searchMedicinesWithCategory(query, selectedCategory).collectLatest { list ->
                medicines.clear()
                medicines.addAll(list)
                adapter.notifyDataSetChanged()

                findViewById<TextView>(R.id.tvCount).text = "${list.size} medicines"
                findViewById<TextView>(R.id.tvEmpty).visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun confirmDelete(med: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("Delete Medicine")
            .setMessage("Delete '${med.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    // Use repository so Firestore is also updated
                    medicineRepository.delete(med)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}