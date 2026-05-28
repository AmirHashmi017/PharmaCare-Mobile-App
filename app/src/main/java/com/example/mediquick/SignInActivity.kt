package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth      = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        db        = AppDatabase.getDatabase(this)

        val etEmail    = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tvError    = findViewById<TextView>(R.id.tvError)

        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email == "admin@gmail.com" && password == "Admin123") {
                loginAsAdmin()
                return@setOnClickListener
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val uid    = result.user?.uid ?: return@launch

                    val doc  = firestore.collection("users").document(uid).get().await()
                    val user = doc.toObject(User::class.java)

                    if (user != null) {
                        db.userDao().insertUser(user)
                        // Delete old legacy SQLite DB (source of "Bruffin" ghost data)
                        DatabaseHelper.deleteLegacyDatabase(this@SignInActivity)
                        UserPrefs.setLoggedIn(this@SignInActivity, true)
                        navigateToMain(user.name)
                    } else {
                        tvError.text       = "User data not found"
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    tvError.text       = e.message
                    tvError.visibility = View.VISIBLE
                }
            }
        }

        findViewById<TextView>(R.id.tvGoSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun loginAsAdmin() {
        val adminUser = User(
            uid   = "ADMIN_ID",
            name  = "System Admin",
            email = "admin@gmail.com",
            role  = UserRole.ADMIN
        )
        lifecycleScope.launch {
            db.userDao().insertUser(adminUser)
            // Delete old legacy SQLite DB
            DatabaseHelper.deleteLegacyDatabase(this@SignInActivity)
            UserPrefs.setLoggedIn(this@SignInActivity, true)
            navigateToMain("Admin")
        }
    }

    private fun navigateToMain(name: String) {
        Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}