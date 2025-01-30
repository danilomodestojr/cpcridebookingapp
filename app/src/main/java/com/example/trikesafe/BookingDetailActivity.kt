//BookingDetailActivity.kt

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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookingDetailActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private var booking: Booking? = null
    private var isActiveBooking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

        // Add logout button handling
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            showLogoutConfirmation()
        }

        // Check if this is an active booking
        isActiveBooking = intent.getBooleanExtra("is_active", false)

        // Setup back button - hide if active booking
        findViewById<ImageButton>(R.id.backButton).apply {
            visibility = if (isActiveBooking) View.GONE else View.VISIBLE
            setOnClickListener {
                finish()
            }
        }

        Log.d("BookingDetailActivity", "Started activity")

        // Get booking from intent first
        booking = intent.getSerializableExtra("booking") as? Booking
        Log.d("BookingDetailActivity", "Received booking: $booking")

        if (booking == null) {
            Log.e("BookingDetailActivity", "No booking data received")
            finish()
            return
        }

        // Initialize map
        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))
        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Display booking details
        displayBookingDetails(booking!!)
        setupMap(booking!!)

        // Setup buttons based on booking state
        setupButtons()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("You can log back in anytime to return to this active booking.")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Clear login preferences
        getSharedPreferences("login_pref", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Return to login screen
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun displayBookingDetails(booking: Booking) {
        Log.d("BookingDetailActivity", "Displaying booking details for tour booking:")
        Log.d("BookingDetailActivity", "Tour name: ${booking.tour_name}")
        Log.d("BookingDetailActivity", "Tour points: ${booking.tour_points}")
        Log.d("BookingDetailActivity", "Booking type: ${booking.booking_type}")

        if (isActiveBooking) {
            // After driver accepts
            findViewById<TextView>(R.id.pickupLocationText).text =
                "Passenger: ${booking.passenger_name ?: "Not available"}"
            findViewById<TextView>(R.id.dropoffLocationText).text =
                "Contact: ${booking.passenger_contact ?: "Not available"}"

            if (booking.booking_type == "tour") {
                // Log what we're getting
                Log.d("BookingDetail", """
            Tour data:
            tour_points: ${booking.tour_points}
            route: ${booking.route}
            dropoff_location: ${booking.dropoff_location}
        """.trimIndent())

                // Use any available tour route information
                val destinations = when {
                    !booking.tour_points.isNullOrEmpty() -> booking.tour_points
                    !booking.route.isNullOrEmpty() -> booking.route
                    !booking.dropoff_location.isNullOrEmpty() -> booking.dropoff_location
                    else -> "Route details not available"
                }

                findViewById<TextView>(R.id.distanceText).text = buildString {
                    append("Tour Package: ${booking.tour_name}\n")
                    append("Destinations: $destinations")
                }
            } else {
                findViewById<TextView>(R.id.distanceText).text =
                    "Distance: ${String.format("%.2f", booking.distance_km)} km"
            }
        } else {
            // Before accepting - show tour info for tour bookings
            if (booking.booking_type == "tour") {
                findViewById<TextView>(R.id.pickupLocationText).text =
                    "Pickup Location: ${booking.pickup_location}"
                findViewById<TextView>(R.id.dropoffLocationText).text =
                    "Tour Package: ${booking.tour_name}"
                findViewById<TextView>(R.id.distanceText).text =
                    "Destinations: ${booking.tour_points ?: booking.route ?: booking.dropoff_location}"
            } else {
                findViewById<TextView>(R.id.pickupLocationText).text =
                    "Pickup Location: ${booking.pickup_location}"
                findViewById<TextView>(R.id.dropoffLocationText).text =
                    "Dropoff Location: ${booking.dropoff_location}"
                findViewById<TextView>(R.id.distanceText).text =
                    "Distance: ${String.format("%.2f", booking.distance_km)} km"
            }
        }

        findViewById<TextView>(R.id.fareText).text =
            if (booking.booking_type == "tour") {
                "Tour Fare: ₱${String.format("%.2f", booking.total_fare)}"
            } else {
                "Fare: ₱${String.format("%.2f", booking.total_fare)}"
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

        // Calculate bounding box for both points
        val boundingBox = calculateBoundingBox(listOf(pickupPoint, dropoffPoint))

        // Apply zoom with padding
        zoomToBox(boundingBox)
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
                Log.e("BookingDetailActivity", "Error zooming to points: ${e.message}")
                // Fallback to default zoom
                map.controller.setZoom(15.0)
                map.controller.setCenter(boundingBox.centerWithDateLine)
            }
        }
    }

    private fun setupButtons() {
        val acceptButton = findViewById<Button>(R.id.acceptButton)
        val completeButton = findViewById<Button>(R.id.completeButton)

        if (isActiveBooking) {
            // Hide accept button, show complete button
            acceptButton.visibility = View.GONE
            completeButton.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    showCompleteConfirmation()
                }
            }
        } else {
            // Show accept button, hide complete button
            acceptButton.visibility = View.VISIBLE
            completeButton.visibility = View.GONE
            acceptButton.setOnClickListener {
                showAcceptConfirmation()
            }
        }
    }

    private fun showCompleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Mark as Complete")
            .setMessage("Have you completed this trip? Note: The booking will only be marked as complete after passenger confirmation.")
            .setPositiveButton("Yes") { _, _ ->
                markBookingAsCompleteByDriver()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun markBookingAsCompleteByDriver() {
        val sharedPreferences = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val driverId = sharedPreferences.getInt("user_id", 0)

        booking?.let { booking ->
            val loadingDialog = AlertDialog.Builder(this)
                .setMessage("Processing your request...")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            ApiClient.getApi(this).markBookingComplete(
                action = "driver_complete",
                bookingId = booking.id,
                driverId = driverId
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        if (response.body()?.message?.contains("completed", ignoreCase = true) == true) {
                            // Booking is completed by passenger, return to driver activity
                            showCompletionConfirmed()
                        } else {
                            // Still waiting for passenger confirmation
                            showWaitingForPassengerConfirmation()
                        }
                    } else {
                        showError("Failed to mark booking as complete. Please try again.")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    showError("Network error: ${t.message}")
                }
            })
        }
    }

    private fun showCompletionConfirmed() {
        AlertDialog.Builder(this)
            .setTitle("Booking Completed")
            .setMessage("The passenger has confirmed completion. You can now accept new bookings.")
            .setPositiveButton("OK") { _, _ ->
                // Return to driver activity
                startActivity(Intent(this, DriverActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showWaitingForPassengerConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Marked as Complete")
            .setMessage("You have marked this booking as complete. Please wait for passenger confirmation.")
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun acceptBooking() {
        val sharedPreferences = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val driverId = sharedPreferences.getInt("user_id", 0)

        if (driverId == 0) {
            showError("Driver ID not found. Please login again.")
            return
        }

        booking?.let { currentBooking ->
            // Show loading indicator
            val loadingDialog = AlertDialog.Builder(this)
                .setMessage("Processing your request...")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            ApiClient.getApi(this).acceptBooking(
                action = "accept",
                bookingId = currentBooking.id,
                driverId = driverId
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // Get the updated booking after acceptance
                        ApiClient.getApi(this@BookingDetailActivity).getDriverActiveBooking(driverId)
                            .enqueue(object : Callback<Booking?> {
                                override fun onResponse(call: Call<Booking?>, response: Response<Booking?>) {
                                    loadingDialog.dismiss()
                                    if (response.isSuccessful && response.body() != null) {
                                        val updatedBooking = response.body()!!

                                        val route = updatedBooking.route ?: "Unknown"
                                        val dropoffLocation = updatedBooking.dropoff_location ?: route

                                        Log.d("BookingDetailActivity", "Route: $route")
                                        Log.d("BookingDetailActivity", "Dropoff Location: $dropoffLocation")

                                        val finalBooking = updatedBooking.copy(route = route, dropoff_location = dropoffLocation)
                                        isActiveBooking = true
                                        runOnUiThread {
                                            displayBookingDetails(finalBooking)
                                            setupButtons()
                                            AlertDialog.Builder(this@BookingDetailActivity)
                                                .setTitle("Success")
                                                .setMessage("You have accepted this booking. The passenger will be notified.")
                                                .setPositiveButton("OK", null)
                                                .show()
                                        }
                                    } else {
                                        showSuccessAndFinish()
                                    }
                                }


                                override fun onFailure(call: Call<Booking?>, t: Throwable) {
                                    loadingDialog.dismiss()
                                    showSuccessAndFinish()
                                }
                            })
                    } else {
                        loadingDialog.dismiss()
                        // Show error and return to booking list
                        showBookingTakenError()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    Log.e("BookingDetailActivity", "Error accepting booking: ${t.message}")
                    showError("Network error: ${t.message}")
                }
            })
        }
    }

    // Add this new function
    private fun showBookingTakenError() {
        AlertDialog.Builder(this)
            .setTitle("Booking Unavailable")
            .setMessage("This booking has already been accepted by another driver.")
            .setPositiveButton("OK") { _, _ ->
                // Return to booking list
                startActivity(Intent(this, DriverActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSuccessAndFinish() {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("You have accepted this booking. The passenger will be notified.")
            .setPositiveButton("OK") { _, _ ->
                // Instead of just finishing, restart this activity with is_active=true
                val intent = Intent(this, BookingDetailActivity::class.java)
                intent.putExtra("booking", booking)
                intent.putExtra("is_active", true)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showAcceptConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Accept Booking")
            .setMessage("Are you sure you want to accept this booking?")
            .setPositiveButton("Accept") { _, _ ->
                acceptBooking()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onBackPressed() {
        if (isActiveBooking) {
            AlertDialog.Builder(this)
                .setTitle("Active Booking")
                .setMessage("You have an ongoing booking. You need to complete this booking first.")
                .setPositiveButton("OK", null)
                .show()
        } else {
            super.onBackPressed()
        }
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
