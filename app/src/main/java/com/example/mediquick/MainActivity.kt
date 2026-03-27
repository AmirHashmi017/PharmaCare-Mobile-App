// app/src/main/java/com/yourpkg/mediquick/MainActivity.kt
package com.example.mediquick

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: replace with your dashboard layout
        val tv = TextView(this)
        val account = UserPrefs.getAccount(this)
        tv.text = "✅ Logged in as ${account?.getString("name")}\n${account?.getString("pharmacy")}"
        tv.textSize = 20f
        tv.setPadding(64, 120, 64, 0)
        setContentView(tv)
    }
}