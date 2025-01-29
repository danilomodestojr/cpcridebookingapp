package com.example.trikesafe

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ServerSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_settings)

        // Get current settings
        val sharedPreferences = getSharedPreferences("server_config", Context.MODE_PRIVATE)
        val currentIp = sharedPreferences.getString("ip_address", "192.168.254.108")
        val currentPort = sharedPreferences.getString("port", "80")

        // Set current values
        findViewById<EditText>(R.id.ipAddressInput).setText(currentIp)
        findViewById<EditText>(R.id.portInput).setText(currentPort)

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val ipAddress = findViewById<EditText>(R.id.ipAddressInput).text.toString()
            val port = findViewById<EditText>(R.id.portInput).text.toString()

            if (ipAddress.isNotEmpty()) {
                sharedPreferences.edit().apply {
                    putString("ip_address", ipAddress)
                    putString("port", port)
                    apply()
                }
                // Reinitialize ApiClient with new settings
                ApiClient.initialize(this)
                Toast.makeText(this, "Server settings saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter IP address", Toast.LENGTH_SHORT).show()
            }
        }
    }
}