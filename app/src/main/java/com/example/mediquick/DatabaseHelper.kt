package com.example.mediquick

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * DEPRECATED — This class is no longer used.
 * The app now uses Room (AppDatabase) exclusively.
 * This file is kept only to avoid compile errors if referenced elsewhere,
 * but the old "mediquick.db" SQLite file is deleted on first run via
 * the deleteLegacyDatabase() call in SignInActivity.
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME    = "mediquick.db"
        private const val DB_VERSION = 2

        /**
         * Call this once after login to wipe the old SQLite DB that may
         * contain stale seed data (e.g. "Bruffin").
         */
        fun deleteLegacyDatabase(context: Context) {
            try {
                context.deleteDatabase(DB_NAME)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) { /* no-op */ }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { /* no-op */ }
}