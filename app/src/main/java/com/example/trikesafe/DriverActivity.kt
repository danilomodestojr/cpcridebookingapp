package com.example.trikesafe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DriverActivity : AppCompatActivity() {
    private lateinit var bookingsList: RecyclerView
    private lateinit var bookingsAdapter: BookingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        // Initialize RecyclerView
        bookingsList = findViewById(R.id.bookingsList)
        bookingsList.layoutManager = LinearLayoutManager(this)
        bookingsAdapter = BookingsAdapter { booking ->
            try {
                Log.d("DriverActivity", "Clicked booking: $booking")
                val intent = Intent(this, BookingDetailActivity::class.java)
                intent.putExtra("booking_id", booking.id)
                intent.putExtra("booking", booking)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("DriverActivity", "Error starting BookingDetailActivity", e)
            }
        }
        bookingsList.adapter = bookingsAdapter

        // Setup logout button
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            getSharedPreferences("login_pref", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Load pending bookings
        loadPendingBookings()
    }

    private fun loadPendingBookings() {
        ApiClient.api.getPendingBookings().enqueue(object : Callback<List<Booking>> {
            override fun onResponse(call: Call<List<Booking>>, response: Response<List<Booking>>) {
                if (response.isSuccessful) {
                    response.body()?.let { bookings ->
                        bookingsAdapter.updateBookings(bookings)
                    }
                } else {
                    showError("Failed to load bookings")
                }
            }

            override fun onFailure(call: Call<List<Booking>>, t: Throwable) {
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}