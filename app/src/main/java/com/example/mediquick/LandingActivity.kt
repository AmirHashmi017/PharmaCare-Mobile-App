// app/src/main/java/com/yourpkg/mediquick/LandingActivity.kt
package com.example.mediquick

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-forward if already signed in
        if (UserPrefs.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_landing)

        // "Get Started" → Sign Up (only if no account yet)
        // If account exists send to Sign In instead
        findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            if (UserPrefs.hasAccount(this)) {
                startActivity(Intent(this, SignInActivity::class.java))
            } else {
                startActivity(Intent(this, SignUpActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}