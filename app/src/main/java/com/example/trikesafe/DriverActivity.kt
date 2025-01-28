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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DriverActivity : AppCompatActivity() {
    private lateinit var bookingsList: RecyclerView
    private lateinit var bookingsAdapter: BookingsAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for active booking first
        val sharedPreferences = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val driverId = sharedPreferences.getInt("user_id", 0)
        checkActiveBooking(driverId)
    }

    private fun checkActiveBooking(driverId: Int) {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Checking booking status...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        ApiClient.api.getDriverActiveBooking(driverId).enqueue(object : Callback<Booking?> {
            override fun onResponse(call: Call<Booking?>, response: Response<Booking?>) {
                loadingDialog.dismiss()
                if (response.isSuccessful) {
                    response.body()?.let { booking ->
                        // Has active booking, go to map view
                        startBookingMapActivity(booking)
                        finish() // Close this activity
                    } ?: run {
                        // No active booking, initialize booking list
                        initializeBookingsList()
                    }
                } else {
                    showError("Failed to check booking status")
                    initializeBookingsList()
                }
            }

            override fun onFailure(call: Call<Booking?>, t: Throwable) {
                loadingDialog.dismiss()
                showError("Network error: ${t.message}")
                initializeBookingsList()
            }
        })
    }

    private fun startBookingMapActivity(booking: Booking) {
        val intent = Intent(this, BookingDetailActivity::class.java)
        intent.putExtra("booking", booking)
        intent.putExtra("is_active", true)
        startActivity(intent)
    }

    private fun initializeBookingsList() {
        setContentView(R.layout.activity_driver)

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            loadPendingBookings()
        }

        // Set refresh indicator colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_blue,
            R.color.primary
        )

        // Initialize RecyclerView
        bookingsList = findViewById(R.id.bookingsList)
        bookingsList.layoutManager = LinearLayoutManager(this)
        bookingsAdapter = BookingsAdapter { booking ->
            val intent = Intent(this, BookingDetailActivity::class.java)
            intent.putExtra("booking_id", booking.id)
            intent.putExtra("booking", booking)
            startActivity(intent)
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

        // Initial load of bookings
        loadPendingBookings()
    }

    private fun loadPendingBookings() {
        ApiClient.api.getPendingBookings().enqueue(object : Callback<List<Booking>> {
            override fun onResponse(call: Call<List<Booking>>, response: Response<List<Booking>>) {
                // Stop the refresh animation
                swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    response.body()?.let { bookings ->
                        bookingsAdapter.updateBookings(bookings)
                    }
                } else {
                    showError("Failed to load bookings")
                }
            }

            override fun onFailure(call: Call<List<Booking>>, t: Throwable) {
                // Stop the refresh animation
                swipeRefreshLayout.isRefreshing = false
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