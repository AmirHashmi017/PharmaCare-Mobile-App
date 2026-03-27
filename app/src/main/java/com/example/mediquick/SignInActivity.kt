
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

class SignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val etPhone    = findViewById<TextInputEditText>(R.id.etPhone)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tvError    = findViewById<TextView>(R.id.tvError)
        val tilPhone   = findViewById<TextInputLayout>(R.id.tilPhone)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)

        fun showError(msg: String) {
            tvError.text = msg
            tvError.visibility = View.VISIBLE
        }

        fun clearErrors() {
            tvError.visibility = View.GONE
            tilPhone.error = null
            tilPassword.error = null
        }

        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            clearErrors()
            val phone    = etPhone.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                phone.isEmpty()    -> { tilPhone.error = "Enter phone number"; return@setOnClickListener }
                password.isEmpty() -> { tilPassword.error = "Enter password"; return@setOnClickListener }
            }

            if (!UserPrefs.hasAccount(this)) {
                showError("No account found on this device. Please sign up first.")
                return@setOnClickListener
            }

            if (UserPrefs.validate(this, phone, password)) {
                UserPrefs.setLoggedIn(this, true)
                val name = UserPrefs.getAccount(this)?.getString("name") ?: "Pharmacist"
                Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } else {
                showError("Incorrect phone number or password.")
            }
        }


        val tvGoSignUp = findViewById<TextView>(R.id.tvGoSignUp)
        if (UserPrefs.hasAccount(this)) {
            tvGoSignUp.visibility = View.GONE
        } else {
            tvGoSignUp.setOnClickListener {
                startActivity(Intent(this, SignUpActivity::class.java))
                finish()
            }
        }
    }
}