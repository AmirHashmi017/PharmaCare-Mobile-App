package com.example.mediquick

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class AddEditMedicineActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var editId: Long = -1
    private var selectedExpiry = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_medicine)

        db = DatabaseHelper(this)

        editId = intent.getLongExtra("medicine_id", -1)
        val isEdit = editId != -1L

        findViewById<TextView>(R.id.tvTitle).text = if (isEdit) "Edit Medicine" else "Add Medicine"
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val etName     = findViewById<TextInputEditText>(R.id.etName)
        val etCategory = findViewById<TextInputEditText>(R.id.etCategory)
        val etPrice    = findViewById<TextInputEditText>(R.id.etPrice)
        val etStock    = findViewById<TextInputEditText>(R.id.etStock)
        val etMinStock = findViewById<TextInputEditText>(R.id.etMinStock)
        val etMfr      = findViewById<TextInputEditText>(R.id.etManufacturer)
        val spinUnit   = findViewById<Spinner>(R.id.spinnerUnit)
        val tvExpiry   = findViewById<TextView>(R.id.tvExpiry)
        val btnSave    = findViewById<Button>(R.id.btnSave)


        val units = listOf("tablets", "capsules", "ml", "mg", "sachets", "bottles", "strips", "pcs")
        spinUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)


        tvExpiry.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedExpiry = "%04d-%02d-%02d".format(y, m + 1, d)
                tvExpiry.text = selectedExpiry
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }


        if (isEdit) {
            db.getMedicineById(editId)?.let { m ->
                etName.setText(m.name)
                etCategory.setText(m.category)
                etPrice.setText(m.price.toString())
                etStock.setText(m.stock.toString())
                etMinStock.setText(m.minStock.toString())
                etMfr.setText(m.manufacturer)
                selectedExpiry = m.expiryDate
                tvExpiry.text  = m.expiryDate
                val unitIdx = units.indexOf(m.unit).coerceAtLeast(0)
                spinUnit.setSelection(unitIdx)
            }
        }

        btnSave.setOnClickListener {
            val name     = etName.text.toString().trim()
            val category = etCategory.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            val stockStr = etStock.text.toString().trim()
            val minStr   = etMinStock.text.toString().trim()
            val mfr      = etMfr.text.toString().trim()
            val unit     = spinUnit.selectedItem.toString()


            if (name.isEmpty())          { tilError(R.id.tilName, "Required"); return@setOnClickListener }
            if (category.isEmpty())      { tilError(R.id.tilCategory, "Required"); return@setOnClickListener }
            if (priceStr.isEmpty())      { tilError(R.id.tilPrice, "Required"); return@setOnClickListener }
            if (stockStr.isEmpty())      { tilError(R.id.tilStock, "Required"); return@setOnClickListener }
            if (selectedExpiry.isEmpty()){ Toast.makeText(this, "Select expiry date", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val med = Medicine(
                id           = editId.coerceAtLeast(0),
                name         = name,
                category     = category,
                price        = priceStr.toDoubleOrNull() ?: 0.0,
                stock        = stockStr.toIntOrNull() ?: 0,
                unit         = unit,
                expiryDate   = selectedExpiry,
                manufacturer = mfr,
                minStock     = minStr.toIntOrNull() ?: 10
            )

            if (isEdit) db.updateMedicine(med) else db.insertMedicine(med)
            Toast.makeText(this, if (isEdit) "Medicine updated!" else "Medicine added!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun tilError(tilId: Int, msg: String) {
        findViewById<TextInputLayout>(tilId).error = msg
    }
}
