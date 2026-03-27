// app/src/main/java/com/yourpkg/mediquick/UserPrefs.kt
package com.example.mediquick

import android.content.Context
import org.json.JSONObject

object UserPrefs {

    private const val PREF_FILE = "mediquick_prefs"
    private const val KEY_USER  = "user_account"
    private const val KEY_LOGGED_IN = "is_logged_in"

    // ── Save new account (only ever one) ──────────────────────────────────
    fun saveAccount(context: Context, name: String, pharmacy: String,
                    phone: String, password: String) {
        val json = JSONObject().apply {
            put("name",     name)
            put("pharmacy", pharmacy)
            put("phone",    phone)
            put("password", password)
        }
        prefs(context).edit().putString(KEY_USER, json.toString()).apply()
    }

    // ── Check if an account already exists on this device ─────────────────
    fun hasAccount(context: Context): Boolean =
        prefs(context).getString(KEY_USER, null) != null

    // ── Validate credentials ───────────────────────────────────────────────
    fun validate(context: Context, phone: String, password: String): Boolean {
        val raw = prefs(context).getString(KEY_USER, null) ?: return false
        val json = JSONObject(raw)
        return json.getString("phone") == phone &&
                json.getString("password") == password
    }

    // ── Session helpers ────────────────────────────────────────────────────
    fun setLoggedIn(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, value).apply()

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOGGED_IN, false)

    // ── Read account info ──────────────────────────────────────────────────
    fun getAccount(context: Context): JSONObject? {
        val raw = prefs(context).getString(KEY_USER, null) ?: return null
        return JSONObject(raw)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
}