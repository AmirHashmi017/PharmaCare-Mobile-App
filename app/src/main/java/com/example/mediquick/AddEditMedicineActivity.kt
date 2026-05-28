package com.example.mediquick

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*

class AddEditMedicineActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var medicineRepository: MedicineRepository
    private var editId: Long = 0
    private var selectedExpiry = ""
    private var selectedImageUri: Uri? = null
    private val auth = FirebaseAuth.getInstance()

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            selectedImageUri = uri
            renderSelectedImage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_medicine)

        db = AppDatabase.getDatabase(this)
        medicineRepository = MedicineRepository(db.medicineDao())

        editId = intent.getLongExtra("medicine_id", 0)
        val isEdit = editId != 0L

        findViewById<TextView>(R.id.tvTitle).text = if (isEdit) "Edit Medicine" else "Add Medicine"
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val etName         = findViewById<TextInputEditText>(R.id.etName)
        val spinCategory   = findViewById<Spinner>(R.id.spinnerCategory)
        val etPrice        = findViewById<TextInputEditText>(R.id.etPrice)
        val etStock        = findViewById<TextInputEditText>(R.id.etStock)
        val etMinStock     = findViewById<TextInputEditText>(R.id.etMinStock)
        val etMfr          = findViewById<TextInputEditText>(R.id.etManufacturer)
        val spinUnit       = findViewById<Spinner>(R.id.spinnerUnit)
        val tvExpiry       = findViewById<TextView>(R.id.tvExpiry)
        val btnSave        = findViewById<Button>(R.id.btnSave)
        val btnPick        = findViewById<TextView>(R.id.btnPickImage)

        btnPick.setOnClickListener { pickImage.launch(arrayOf("image/*")) }

        spinCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, MedicineConstants.CATEGORIES)
        spinUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, MedicineConstants.UNITS)

        tvExpiry.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedExpiry = "%04d-%02d-%02d".format(y, m + 1, d)
                tvExpiry.text = selectedExpiry
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        if (isEdit) {
            lifecycleScope.launch {
                val medicine = medicineRepository.getMedicineById(editId)
                medicine?.let { m ->
                    etName.setText(m.name)
                    val catIdx = MedicineConstants.CATEGORIES.indexOf(m.category).coerceAtLeast(0)
                    spinCategory.setSelection(catIdx)
                    etPrice.setText(m.price.toString())
                    etStock.setText(m.stock.toString())
                    etMinStock.setText(m.minStock.toString())
                    etMfr.setText(m.manufacturer)
                    selectedExpiry = m.expiryDate
                    tvExpiry.text  = m.expiryDate
                    val unitIdx = MedicineConstants.UNITS.indexOf(m.unit).coerceAtLeast(0)
                    spinUnit.setSelection(unitIdx)
                    m.imageUri?.let {
                        selectedImageUri = Uri.parse(it)
                        renderSelectedImage()
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            val name     = etName.text.toString().trim()
            val category = spinCategory.selectedItem.toString()
            val priceStr = etPrice.text.toString().trim()
            val stockStr = etStock.text.toString().trim()
            val minStr   = etMinStock.text.toString().trim()
            val mfr      = etMfr.text.toString().trim()
            val unit     = spinUnit.selectedItem.toString()

            if (name.isEmpty()) { tilError(R.id.tilName, "Required"); return@setOnClickListener }
            if (priceStr.isEmpty()) { tilError(R.id.tilPrice, "Required"); return@setOnClickListener }
            if (stockStr.isEmpty()) { tilError(R.id.tilStock, "Required"); return@setOnClickListener }
            if (selectedExpiry.isEmpty()){ Toast.makeText(this, "Select expiry date", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val currentUid = auth.currentUser?.uid ?: "ADMIN_ID"

            val med = Medicine(
                id           = editId,
                name         = name,
                category     = category,
                price        = priceStr.toDoubleOrNull() ?: 0.0,
                stock        = stockStr.toIntOrNull() ?: 0,
                unit         = unit,
                expiryDate   = selectedExpiry,
                manufacturer = mfr,
                minStock     = minStr.toIntOrNull() ?: 10,
                imageUri     = selectedImageUri?.toString(),
                addedBy      = if (isEdit) null else currentUid,
                lastUpdatedBy = currentUid
            )

            lifecycleScope.launch {
                if (isEdit) {
                    val existing = medicineRepository.getMedicineById(editId)
                    val toUpdate = med.copy(addedBy = existing?.addedBy ?: currentUid)
                    medicineRepository.update(toUpdate)
                } else {
                    medicineRepository.insert(med)
                }

                runOnUiThread {
                    Toast.makeText(
                        this@AddEditMedicineActivity,
                        if (isEdit) "Medicine updated!" else "Medicine added!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun renderSelectedImage() {
        val iv = findViewById<ImageView>(R.id.ivMedicineImage)
        val tvFallback = findViewById<TextView>(R.id.tvMedicineImageFallback)
        val tvStatus = findViewById<TextView>(R.id.tvMedicineImageStatus)
        val btnPick = findViewById<TextView>(R.id.btnPickImage)

        val uri = selectedImageUri
        if (uri == null) {
            iv.visibility = View.GONE
            tvFallback.visibility = View.VISIBLE
            tvStatus.text = "Optional"
            btnPick.text = "Add Photo"
            return
        }

        try {
            iv.setImageURI(uri)
            iv.visibility = View.VISIBLE
            tvFallback.visibility = View.GONE
            tvStatus.text = "Image selected"
            btnPick.text = "Change Photo"
        } catch (e: Exception) {
            iv.visibility = View.GONE
            tvFallback.visibility = View.VISIBLE
            tvStatus.text = "Error loading image"
        }
    }

    private fun tilError(tilId: Int, msg: String) {
        findViewById<TextInputLayout>(tilId).error = msg
    }
}