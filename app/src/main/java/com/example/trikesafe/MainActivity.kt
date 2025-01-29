package com.example.trikesafe


import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if already logged in
        val sharedPref = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val loggedInRole = sharedPref.getString("user_role", null)

        if (loggedInRole != null) {
            // If user_role is found, go directly to the correct screen
            when (loggedInRole) {
                "driver" -> {
                    startActivity(Intent(this, DriverActivity::class.java))
                    finish()
                }
                "passenger" -> {
                    startActivity(Intent(this, PassengerActivity::class.java))
                    finish()
                }
            }
            return
        }

        setContentView(R.layout.activity_main)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val registerLink = findViewById<TextView>(R.id.registerLink)
        val serverSettingsButton = findViewById<Button>(R.id.serverSettingsButton)

        // Initialize ApiClient with context
        ApiClient.initialize(this)

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

        serverSettingsButton.setOnClickListener {
            startActivity(Intent(this, ServerSettingsActivity::class.java))
        }
    }

    private fun loginUser(username: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@MainActivity).loginUser(LoginRequest(username, password))
                if (response.isSuccessful) {
                    response.body()?.let { handleLoginResponse(it) }
                } else {
                    showConnectionError()
                }
            } catch (e: Exception) {
                showConnectionError()
            }
        }
    }

    private fun showConnectionError() {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Connection Error")
                .setMessage("Could not connect to server. Would you like to check server settings?")
                .setPositiveButton("Settings") { _, _ ->
                    startActivity(Intent(this, ServerSettingsActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun handleLoginResponse(response: ApiResponse) {
        if (response.success) {
            // Save login state (user role AND user ID!)
            val sharedPref = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("user_role", response.role?.lowercase()) // store the role
                putInt("user_id", response.userId ?: 0)            // store the user ID
                apply()
            }

            when (response.role?.lowercase()) {
                "driver" -> {
                    startActivity(Intent(this, DriverActivity::class.java))
                    finish()
                }
                "passenger" -> {
                    startActivity(Intent(this, PassengerActivity::class.java))
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Invalid role: ${response.role}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Show error if login unsuccessful
            Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
        }
    }
}