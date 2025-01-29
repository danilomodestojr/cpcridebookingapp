package com.example.trikesafe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.osmdroid.util.BoundingBox

class PassengerBookingDetailActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private var booking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ApiClient
        ApiClient.initialize(this)

        setContentView(R.layout.activity_passenger_booking_detail)


        // Setup logout button
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            showLogoutConfirmation()
        }

        Log.d("PassengerBookingDetail", "Started activity")

        // Get booking from intent
        booking = intent.getSerializableExtra("booking") as? Booking
        Log.d("PassengerBookingDetail", "Received booking: $booking")

        if (booking == null) {
            Log.e("PassengerBookingDetail", "No booking data received")
            finish()
            return
        }

        // Initialize map
        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))
        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Display booking details and setup map
        displayBookingDetails(booking!!)
        setupMap(booking!!)

        setupRefreshButton()

        // Setup completion confirmation button
        setupConfirmButton()

        // Disable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun displayBookingDetails(booking: Booking) {
        when (booking.status) {
            "pending" -> {
                // Show locations while waiting for driver
                findViewById<TextView>(R.id.driverNameText).text =
                    "Driver: Waiting for driver..."
                findViewById<TextView>(R.id.pickupLocationText).text =
                    "Pickup Location: ${booking.pickup_location}"
                findViewById<TextView>(R.id.dropoffLocationText).text =
                    "Dropoff Location: ${booking.dropoff_location}"
            }
            "accepted" -> {
                // Show driver details when accepted
                findViewById<TextView>(R.id.driverNameText).text =
                    "Driver: ${booking.driver_name ?: "Unknown"}"
                findViewById<TextView>(R.id.pickupLocationText).text =
                    "Contact: ${booking.driver_contact ?: "Not available"}"
                findViewById<TextView>(R.id.dropoffLocationText).text =
                    "Trip Status: In Progress"
            }
            else -> {
                findViewById<TextView>(R.id.driverNameText).text =
                    "Driver: ${booking.driver_name ?: "Unknown"}"
                findViewById<TextView>(R.id.pickupLocationText).text =
                    "Status: ${booking.status.capitalize()}"
                findViewById<TextView>(R.id.dropoffLocationText).text = ""
            }
        }

        findViewById<TextView>(R.id.fareText).text =
            "Fare: ₱${String.format("%.2f", booking.total_fare)}"

        // Update status text
        val statusText = findViewById<TextView>(R.id.statusText)
        when (booking.status) {
            "pending" -> statusText.text = "Waiting for Driver"
            "accepted" -> statusText.text = "Trip in Progress"
            else -> statusText.text = booking.status.capitalize()
        }
    }

    private fun setupMap(booking: Booking) {
        val pickupPoint = GeoPoint(booking.pickup_latitude, booking.pickup_longitude)
        val dropoffPoint = GeoPoint(booking.dropoff_latitude, booking.dropoff_longitude)

        // Add pickup marker
        val pickupMarker = Marker(map).apply {
            position = pickupPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Pickup Location"
            snippet = booking.pickup_location
            icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation)
        }
        map.overlays.add(pickupMarker)

        // Add dropoff marker
        val dropoffMarker = Marker(map).apply {
            position = dropoffPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Dropoff Location"
            snippet = booking.dropoff_location
            icon = resources.getDrawable(android.R.drawable.ic_menu_myplaces)
        }
        map.overlays.add(dropoffMarker)

        // Calculate bounding box and zoom to fit both points
        val boundingBox = calculateBoundingBox(listOf(pickupPoint, dropoffPoint))
        zoomToBox(boundingBox)
    }

    private fun setupConfirmButton() {
        val confirmButton = findViewById<Button>(R.id.confirmButton)

        // Only show confirm button if booking is accepted and driver has marked as complete
        confirmButton.visibility = if (booking?.status == "accepted") View.VISIBLE else View.GONE

        confirmButton.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Trip Completion")
            .setMessage("Please confirm that you have reached your destination and the trip is complete.")
            .setPositiveButton("Confirm") { _, _ ->
                confirmTripCompletion()
            }
            .setNegativeButton("Not Yet", null)
            .show()
    }

    private fun confirmTripCompletion() {
        val sharedPreferences = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val passengerId = sharedPreferences.getInt("user_id", 0)

        booking?.let { booking ->
            val loadingDialog = AlertDialog.Builder(this)
                .setMessage("Processing confirmation...")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            ApiClient.getApi(this).confirmBookingComplete(
                action = "passenger_confirm",
                bookingId = booking.id,
                passengerId = passengerId
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        showCompletionSuccess()
                    } else {
                        showError("Failed to confirm completion. Please try again.")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    showError("Network error: ${t.message}")
                }
            })
        }
    }

    private fun showCompletionSuccess() {
        AlertDialog.Builder(this)
            .setTitle("Trip Completed")
            .setMessage("Thank you for using our service!")
            .setPositiveButton("OK") { _, _ ->
                // Return to main passenger screen
                startActivity(Intent(this, PassengerActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("You can log back in anytime to return to this booking.")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        getSharedPreferences("login_pref", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun calculateBoundingBox(points: List<GeoPoint>): BoundingBox {
        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var minLon = Double.POSITIVE_INFINITY
        var maxLon = Double.NEGATIVE_INFINITY

        points.forEach { point ->
            minLat = minOf(minLat, point.latitude)
            maxLat = maxOf(maxLat, point.latitude)
            minLon = minOf(minLon, point.longitude)
            maxLon = maxOf(maxLon, point.longitude)
        }

        // Add a small padding (about 20% of the range)
        val latPadding = (maxLat - minLat) * 0.2
        val lonPadding = (maxLon - minLon) * 0.2

        return BoundingBox(
            maxLat + latPadding,  // north
            maxLon + lonPadding,  // east
            minLat - latPadding,  // south
            minLon - lonPadding   // west
        )
    }

    private fun zoomToBox(boundingBox: BoundingBox) {
        map.post {
            try {
                // Get the map's dimensions
                val mapWidth = map.width
                val mapHeight = map.height

                if (mapWidth > 0 && mapHeight > 0) {
                    // Animate zoom to bounding box
                    map.zoomToBoundingBox(boundingBox, true, 50)

                    // Ensure minimum zoom level for urban area visibility
                    if (map.zoomLevelDouble < 14.0) {
                        map.controller.setZoom(14.0)
                    }
                } else {
                    // Fallback if map dimensions aren't ready
                    map.controller.setZoom(15.0)
                    map.controller.setCenter(boundingBox.centerWithDateLine)
                }
            } catch (e: Exception) {
                Log.e("PassengerBookingDetail", "Error zooming to points: ${e.message}")
                // Fallback to default zoom
                map.controller.setZoom(15.0)
                map.controller.setCenter(boundingBox.centerWithDateLine)
            }
        }
    }

    private fun setupRefreshButton() {
        findViewById<FloatingActionButton>(R.id.refreshButton).setOnClickListener {
            refreshBookingStatus()
        }
    }

    private fun refreshBookingStatus() {
        val sharedPreferences = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val passengerId = sharedPreferences.getInt("user_id", 0)

        // Show loading indicator
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Refreshing booking status...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        ApiClient.getApi(this).getPassengerActiveBooking(passengerId).enqueue(object : Callback<Booking?> {
            override fun onResponse(call: Call<Booking?>, response: Response<Booking?>) {
                runOnUiThread {
                    loadingDialog.dismiss()
                    if (response.isSuccessful) {
                        response.body()?.let { updatedBooking ->
                            booking = updatedBooking
                            displayBookingDetails(updatedBooking)
                            // Show driver found message if status changed to accepted
                            if (updatedBooking.status == "accepted") {
                                showDriverFoundMessage()
                                setupConfirmButton() // Update confirm button visibility
                            }
                        }
                    } else {
                        showError("Failed to refresh booking status")
                    }
                }
            }

            override fun onFailure(call: Call<Booking?>, t: Throwable) {
                runOnUiThread {
                    loadingDialog.dismiss()
                    showError("Failed to refresh: ${t.message}")
                }
            }
        })
    }

    private fun showDriverFoundMessage() {
        AlertDialog.Builder(this)
            .setTitle("Driver Found!")
            .setMessage("A driver has accepted your booking. You can now see their details.")
            .setPositiveButton("OK", null)
            .show()
    }

    // Override back button to prevent leaving active booking
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Active Booking")
            .setMessage("You cannot leave while you have an active booking.")
            .setPositiveButton("OK") { _, _ ->
                super.onBackPressed()  // Added super call
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}