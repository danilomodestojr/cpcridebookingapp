package com.example.trikesafe

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if already logged in
        val sharedPref = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val loggedInRole = sharedPref.getString("user_role", null)

        if (loggedInRole != null) {
            when (loggedInRole) {
                "driver" -> startActivity(Intent(this, DriverActivity::class.java))
                "passenger" -> startActivity(Intent(this, PassengerActivity::class.java))
            }
            finish()
            return
        }


        setContentView(R.layout.activity_main)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val registerLink = findViewById<TextView>(R.id.registerLink)

        loginButton.setOnClickListener {
            val username = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(username, password)
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(username: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.loginUser(LoginRequest(username, password))
                if (response.isSuccessful) {
                    response.body()?.let { handleLoginResponse(it) }
                } else {
                    Toast.makeText(this@MainActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleLoginResponse(response: ApiResponse) {
        if (response.success) {

            // Save login state
            val sharedPref = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("user_role", response.role?.lowercase())
                apply()
            }

            when (response.role) {
                "driver" -> {
                    startActivity(Intent(this, DriverActivity::class.java))
                    finish()  // Add this
                }
                "passenger" -> {
                    startActivity(Intent(this, PassengerActivity::class.java))
                    finish()  // Add this
                }
                else -> Toast.makeText(this, "Invalid role", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
        }
    }
}