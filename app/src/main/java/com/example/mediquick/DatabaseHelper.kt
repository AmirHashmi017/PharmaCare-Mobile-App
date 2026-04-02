package com.example.mediquick

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME    = "mediquick.db"
        private const val DB_VERSION = 2


        const val TBL_MED   = "medicines"
        const val COL_ID    = "id"
        const val COL_NAME  = "name"
        const val COL_CAT   = "category"
        const val COL_PRICE = "price"
        const val COL_STOCK = "stock"
        const val COL_UNIT  = "unit"
        const val COL_EXP   = "expiry_date"
        const val COL_MFR   = "manufacturer"
        const val COL_MIN   = "min_stock"
        const val COL_IMG   = "image_uri"


        const val TBL_SALE  = "sales"
        const val COL_CUST  = "customer_name"
        const val COL_DATE  = "date"
        const val COL_TOTAL = "total"


        const val TBL_ITEM  = "sale_items"
        const val COL_SID   = "sale_id"
        const val COL_MID   = "medicine_id"
        const val COL_MNAME = "medicine_name"
        const val COL_QTY   = "quantity"
        const val COL_EACH  = "price_each"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TBL_MED (
                $COL_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME  TEXT NOT NULL,
                $COL_CAT   TEXT NOT NULL,
                $COL_PRICE REAL NOT NULL,
                $COL_STOCK INTEGER NOT NULL DEFAULT 0,
                $COL_UNIT  TEXT NOT NULL DEFAULT 'tablets',
                $COL_EXP   TEXT NOT NULL,
                $COL_MFR   TEXT NOT NULL,
                $COL_MIN   INTEGER NOT NULL DEFAULT 10,
                $COL_IMG   TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TBL_SALE (
                $COL_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CUST  TEXT NOT NULL,
                $COL_DATE  TEXT NOT NULL,
                $COL_TOTAL REAL NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TBL_ITEM (
                $COL_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SID   INTEGER NOT NULL,
                $COL_MID   INTEGER NOT NULL,
                $COL_MNAME TEXT NOT NULL,
                $COL_QTY   INTEGER NOT NULL,
                $COL_EACH  REAL NOT NULL,
                FOREIGN KEY($COL_SID) REFERENCES $TBL_SALE($COL_ID)
            )
        """.trimIndent())


        seedSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TBL_MED ADD COLUMN $COL_IMG TEXT")
        }
    }

    private fun seedSampleData(db: SQLiteDatabase) {
        val samples = listOf(
            arrayOf("Paracetamol 500mg", "Analgesic", 12.0, 150, "tablets", "2026-12-31", "PharmaCo", 20),
            arrayOf("Amoxicillin 250mg", "Antibiotic", 35.0, 80, "capsules", "2025-09-30", "MediLabs", 15),
            arrayOf("ORS Sachet", "Rehydration", 8.5, 200, "sachets", "2026-06-30", "Hydra Inc", 30),
            arrayOf("Omeprazole 20mg", "Antacid", 28.0, 60, "capsules", "2025-11-30", "GastroPharma", 10),
            arrayOf("Cetirizine 10mg", "Antihistamine", 15.0, 7, "tablets", "2026-03-31", "AllergyCare", 15),
            arrayOf("Ibuprofen 400mg", "Anti-inflammatory", 18.0, 120, "tablets", "2026-08-31", "PainAway", 20),
            arrayOf("Metformin 500mg", "Antidiabetic", 22.0, 4, "tablets", "2025-10-31", "DiabeCare", 10),
        )
        for (s in samples) {
            val cv = ContentValues().apply {
                put(COL_NAME, s[0] as String)
                put(COL_CAT, s[1] as String)
                put(COL_PRICE, s[2] as Double)
                put(COL_STOCK, s[3] as Int)
                put(COL_UNIT, s[4] as String)
                put(COL_EXP, s[5] as String)
                put(COL_MFR, s[6] as String)
                put(COL_MIN, s[7] as Int)
            }
            db.insert(TBL_MED, null, cv)
        }
    }


    fun insertMedicine(m: Medicine): Long {
        val cv = ContentValues().apply {
            put(COL_NAME, m.name); put(COL_CAT, m.category)
            put(COL_PRICE, m.price); put(COL_STOCK, m.stock)
            put(COL_UNIT, m.unit); put(COL_EXP, m.expiryDate)
            put(COL_MFR, m.manufacturer); put(COL_MIN, m.minStock)
            put(COL_IMG, m.imageUri)
        }
        return writableDatabase.insert(TBL_MED, null, cv)
    }

    fun updateMedicine(m: Medicine): Int {
        val cv = ContentValues().apply {
            put(COL_NAME, m.name); put(COL_CAT, m.category)
            put(COL_PRICE, m.price); put(COL_STOCK, m.stock)
            put(COL_UNIT, m.unit); put(COL_EXP, m.expiryDate)
            put(COL_MFR, m.manufacturer); put(COL_MIN, m.minStock)
            put(COL_IMG, m.imageUri)
        }
        return writableDatabase.update(TBL_MED, cv, "$COL_ID=?", arrayOf(m.id.toString()))
    }

    fun deleteMedicine(id: Long) {
        writableDatabase.delete(TBL_MED, "$COL_ID=?", arrayOf(id.toString()))
    }

    fun getAllMedicines(query: String = "", category: String = ""): List<Medicine> {
        val medicines = mutableListOf<Medicine>()
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (query.isNotBlank()) { conditions.add("$COL_NAME LIKE ?"); args.add("%$query%") }
        if (category.isNotBlank() && category != "All") { conditions.add("$COL_CAT=?"); args.add(category) }
        val where = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TBL_MED $where ORDER BY $COL_NAME ASC", args.toTypedArray()
        )
        cursor.use {
            while (it.moveToNext()) medicines.add(cursorToMedicine(it))
        }
        return medicines
    }

    fun getLowStockMedicines(): List<Medicine> {
        val list = mutableListOf<Medicine>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TBL_MED WHERE $COL_STOCK <= $COL_MIN ORDER BY $COL_STOCK ASC", null
        )
        cursor.use { while (it.moveToNext()) list.add(cursorToMedicine(it)) }
        return list
    }

    fun getMedicineById(id: Long): Medicine? {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TBL_MED WHERE $COL_ID=?", arrayOf(id.toString())
        )
        return cursor.use { if (it.moveToFirst()) cursorToMedicine(it) else null }
    }

    fun getCategories(): List<String> {
        val list = mutableListOf("All")
        val cursor = readableDatabase.rawQuery(
            "SELECT DISTINCT $COL_CAT FROM $TBL_MED ORDER BY $COL_CAT ASC", null
        )
        cursor.use { while (it.moveToNext()) list.add(it.getString(0)) }
        return list
    }

    private fun cursorToMedicine(c: android.database.Cursor) = Medicine(
        id           = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
        name         = c.getString(c.getColumnIndexOrThrow(COL_NAME)),
        category     = c.getString(c.getColumnIndexOrThrow(COL_CAT)),
        price        = c.getDouble(c.getColumnIndexOrThrow(COL_PRICE)),
        stock        = c.getInt(c.getColumnIndexOrThrow(COL_STOCK)),
        unit         = c.getString(c.getColumnIndexOrThrow(COL_UNIT)),
        expiryDate   = c.getString(c.getColumnIndexOrThrow(COL_EXP)),
        manufacturer = c.getString(c.getColumnIndexOrThrow(COL_MFR)),
        minStock     = c.getInt(c.getColumnIndexOrThrow(COL_MIN)),
        imageUri     = c.getString(c.getColumnIndexOrThrow(COL_IMG)).takeUnless { it.isNullOrBlank() }
    )


    fun insertSale(sale: Sale, items: List<CartItem>): Long {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val cv = ContentValues().apply {
                put(COL_CUST, sale.customerName)
                put(COL_DATE, sale.date)
                put(COL_TOTAL, sale.total)
            }
            val saleId = db.insert(TBL_SALE, null, cv)
            for (item in items) {
                val iv = ContentValues().apply {
                    put(COL_SID, saleId)
                    put(COL_MID, item.medicine.id)
                    put(COL_MNAME, item.medicine.name)
                    put(COL_QTY, item.quantity)
                    put(COL_EACH, item.medicine.price)
                }
                db.insert(TBL_ITEM, null, iv)

                db.execSQL(
                    "UPDATE $TBL_MED SET $COL_STOCK = $COL_STOCK - ? WHERE $COL_ID = ?",
                    arrayOf(item.quantity, item.medicine.id)
                )
            }
            db.setTransactionSuccessful()
            saleId
        } finally {
            db.endTransaction()
        }
    }

    fun getAllSales(): List<Sale> {
        val list = mutableListOf<Sale>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TBL_SALE ORDER BY $COL_DATE DESC", null
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COL_ID))
                list.add(
                    Sale(
                        id           = id,
                        customerName = it.getString(it.getColumnIndexOrThrow(COL_CUST)),
                        date         = it.getString(it.getColumnIndexOrThrow(COL_DATE)),
                        total        = it.getDouble(it.getColumnIndexOrThrow(COL_TOTAL)),
                        items        = getSaleItems(id)
                    )
                )
            }
        }
        return list
    }

    fun getSaleItems(saleId: Long): List<SaleItem> {
        val list = mutableListOf<SaleItem>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TBL_ITEM WHERE $COL_SID=?", arrayOf(saleId.toString())
        )
        cursor.use {
            while (it.moveToNext()) list.add(
                SaleItem(
                    id           = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                    saleId       = saleId,
                    medicineId   = it.getLong(it.getColumnIndexOrThrow(COL_MID)),
                    medicineName = it.getString(it.getColumnIndexOrThrow(COL_MNAME)),
                    quantity     = it.getInt(it.getColumnIndexOrThrow(COL_QTY)),
                    priceEach    = it.getDouble(it.getColumnIndexOrThrow(COL_EACH))
                )
            )
        }
        return list
    }


    fun getTotalMedicines(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM $TBL_MED", null)
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun getLowStockCount(): Int {
        val c = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TBL_MED WHERE $COL_STOCK <= $COL_MIN", null
        )
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun getTodaySalesTotal(): Double {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val c = readableDatabase.rawQuery(
            "SELECT COALESCE(SUM($COL_TOTAL),0) FROM $TBL_SALE WHERE $COL_DATE LIKE ?",
            arrayOf("$today%")
        )
        return c.use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
    }

    fun getTotalSales(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM $TBL_SALE", null)
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }
}
