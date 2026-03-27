package com.example.mediquick

import android.content.Context
import org.json.JSONObject

object UserPrefs {

    private const val PREF_FILE = "mediquick_prefs"
    private const val KEY_USER  = "user_account"
    private const val KEY_LOGGED_IN = "is_logged_in"

    fun saveAccount(context: Context, name: String, pharmacy: String,
                    phone: String, password: String) {
        try {
            val json = JSONObject().apply {
                put("name",     name)
                put("pharmacy", pharmacy)
                put("phone",    phone)
                put("password", password)
            }
            prefs(context).edit().putString(KEY_USER, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasAccount(context: Context): Boolean {
        return try {
            prefs(context).getString(KEY_USER, null) != null
        } catch (e: Exception) {
            false
        }
    }

    fun validate(context: Context, phone: String, password: String): Boolean {
        return try {
            val raw = prefs(context).getString(KEY_USER, null) ?: return false
            val json = JSONObject(raw)
            json.getString("phone") == phone &&
                    json.getString("password") == password
        } catch (e: Exception) {
            false
        }
    }

    fun setLoggedIn(context: Context, value: Boolean) {
        try {
            prefs(context).edit().putBoolean(KEY_LOGGED_IN, value).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        return try {
            prefs(context).getBoolean(KEY_LOGGED_IN, false)
        } catch (e: Exception) {
            false
        }
    }

    fun getAccount(context: Context): JSONObject? {
        return try {
            val raw = prefs(context).getString(KEY_USER, null) ?: return null
            JSONObject(raw)
        } catch (e: Exception) {
            null
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
}