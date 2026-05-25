package com.example.mediquick

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

class AddPharmacistActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pharmacist)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        db = AppDatabase.getDatabase(this)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvError)

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // In a real app, Admin might use Firebase Admin SDK to create users without logging out.
                    // Here, we'll create the user. Note: This might sign out the admin if not handled.
                    // A better way for this demo is to just save to Firestore and let the pharmacist "reset" or "claim" account,
                    // or use a secondary Auth instance if possible.
                    // For simplicity, we'll use the current Auth to create, then the admin would have to log back in if they are signed out.
                    // Or we can just mock it for this Mid-Project transition.
                    
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = result.user?.uid ?: return@launch

                    val pharmacist = User(
                        uid = uid,
                        name = name,
                        email = email,
                        role = UserRole.PHARMACIST,
                        phone = phone,
                        addedByAdmin = true
                    )

                    firestore.collection("users").document(uid).set(pharmacist).await()
                    // Don't insert into local Room yet, as the admin is the one currently logged in locally.
                    
                    Toast.makeText(this@AddPharmacistActivity, "Pharmacist added successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    tvError.text = e.message
                    tvError.visibility = View.VISIBLE
                }
            }
        }
    }
}
