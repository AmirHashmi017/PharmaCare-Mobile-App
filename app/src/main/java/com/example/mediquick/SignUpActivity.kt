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

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        db = AppDatabase.getDatabase(this)

        val etName     = findViewById<TextInputEditText>(R.id.etName)
        val etEmail    = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone    = findViewById<TextInputEditText>(R.id.etPhone)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirm  = findViewById<TextInputEditText>(R.id.etConfirm)
        val tvError    = findViewById<TextView>(R.id.tvError)

        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            val name     = etName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val phone    = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm  = etConfirm.text.toString()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = result.user?.uid ?: return@launch
                    
                    val user = User(
                        uid = uid,
                        name = name,
                        email = email,
                        role = UserRole.USER,
                        phone = phone
                    )

                    firestore.collection("users").document(uid).set(user).await()
                    db.userDao().insertUser(user)
                    
                    UserPrefs.setLoggedIn(this@SignUpActivity, true)
                    
                    Toast.makeText(this@SignUpActivity, "Welcome $name!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignUpActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } catch (e: Exception) {
                    tvError.text = e.message
                    tvError.visibility = View.VISIBLE
                }
            }
        }

        findViewById<TextView>(R.id.tvGoSignIn).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}
