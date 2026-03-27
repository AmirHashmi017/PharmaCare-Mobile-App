// app/src/main/java/com/yourpkg/mediquick/SignUpActivity.kt
package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // One account per device — bounce to sign in if already registered
        if (UserPrefs.hasAccount(this)) {
            Toast.makeText(this, "Account already exists on this device.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_sign_up)

        val etName     = findViewById<TextInputEditText>(R.id.etName)
        val etPharmacy = findViewById<TextInputEditText>(R.id.etPharmacy)
        val etPhone    = findViewById<TextInputEditText>(R.id.etPhone)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirm  = findViewById<TextInputEditText>(R.id.etConfirm)
        val tvError    = findViewById<TextView>(R.id.tvError)

        val tilName     = findViewById<TextInputLayout>(R.id.tilName)
        val tilPharmacy = findViewById<TextInputLayout>(R.id.tilPharmacy)
        val tilPhone    = findViewById<TextInputLayout>(R.id.tilPhone)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirm  = findViewById<TextInputLayout>(R.id.tilConfirm)

        fun showError(msg: String) {
            tvError.text = msg
            tvError.visibility = View.VISIBLE
        }

        fun clearErrors() {
            tvError.visibility = View.GONE
            listOf(tilName, tilPharmacy, tilPhone, tilPassword, tilConfirm)
                .forEach { it.error = null }
        }

        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            clearErrors()
            val name     = etName.text.toString().trim()
            val pharmacy = etPharmacy.text.toString().trim()
            val phone    = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm  = etConfirm.text.toString()

            // Validation
            when {
                name.isEmpty()     -> { tilName.error = "Enter your full name"; return@setOnClickListener }
                pharmacy.isEmpty() -> { tilPharmacy.error = "Enter pharmacy name"; return@setOnClickListener }
                phone.length < 10  -> { tilPhone.error = "Enter a valid phone number"; return@setOnClickListener }
                password.length < 6 -> { tilPassword.error = "Password must be at least 6 characters"; return@setOnClickListener }
                password != confirm -> { tilConfirm.error = "Passwords do not match"; return@setOnClickListener }
            }

            UserPrefs.saveAccount(this, name, pharmacy, phone, password)
            UserPrefs.setLoggedIn(this, true)

            Toast.makeText(this, "Welcome to MediQuick, $name! 🎉", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        findViewById<TextView>(R.id.tvGoSignIn).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}