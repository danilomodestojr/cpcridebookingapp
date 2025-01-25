package com.example.trikesafe

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class DriverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            getSharedPreferences("login_pref", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}