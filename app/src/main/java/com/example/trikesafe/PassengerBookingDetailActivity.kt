package com.example.trikesafe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
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
    private var isActiveBooking: Boolean = false

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

        // Get booking from intent and update isActiveBooking status
        booking = intent.getSerializableExtra("booking") as? Booking
        Log.d("PassengerBookingDetail", "Received booking: $booking")

        if (booking == null) {
            Log.e("PassengerBookingDetail", "No booking data received")
            finish()
            return
        }

        isActiveBooking = booking?.status == "accepted"  // Set based on booking status

        // Initialize map
        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))
        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Display booking details and setup map
        displayBookingDetails(booking!!)
        setupMap(booking!!)

        setupRefreshButton()

        setupButtons()  // Make sure this is called

        // Setup completion confirmation button
        setupConfirmButton()

        // Disable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun displayBookingDetails(booking: Booking) {
        Log.d("PassengerBookingDetail", "Displaying booking details: ${booking.id}")
        Log.d("PassengerBookingDetail", "Booking type: ${booking.booking_type}")
        Log.d("PassengerBookingDetail", "Tour name: ${booking.tour_name}")
        Log.d("PassengerBookingDetail", "Tour points: ${booking.tour_points}")
        Log.d("PassengerBookingDetail", "Status: ${booking.status}")

        if (booking.status == "accepted") {  // Use booking.status directly instead of isActiveBooking
            // Show driver details when accepted
            findViewById<TextView>(R.id.driverNameText).text =
                "Driver: ${booking.driver_name ?: "Unknown"}"
            findViewById<TextView>(R.id.pickupLocationText).text =
                "Contact: ${booking.driver_contact ?: "Not available"}"

            if (booking.booking_type == "tour") {
                findViewById<TextView>(R.id.dropoffLocationText).text = buildString {
                    append("Tour Package: ${booking.tour_name}\n")
                    append("Tour Destinations: ${booking.tour_points}\n")
                    append("Trip Status: In Progress")
                }
            } else {
                findViewById<TextView>(R.id.dropoffLocationText).text =
                    "Trip Status: In Progress"
            }
        } else {
            // Show locations while waiting for driver
            findViewById<TextView>(R.id.driverNameText).text =
                "Driver: Waiting for driver..."
            findViewById<TextView>(R.id.pickupLocationText).text =
                "Pickup Location: ${booking.pickup_location}"

            if (booking.booking_type == "tour") {
                findViewById<TextView>(R.id.dropoffLocationText).text = buildString {
                    append("Tour Package: ${booking.tour_name}\n")
                    append("Tour Destinations: ${booking.tour_points}")
                }
            } else {
                findViewById<TextView>(R.id.dropoffLocationText).text =
                    "Dropoff Location: ${booking.dropoff_location}"
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
            .setMessage("Thank you for using our service! Would you like to provide feedback for your driver?")
            .setPositiveButton("Give Feedback") { _, _ ->
                showFeedbackDialog()
            }
            .setNegativeButton("Skip") { _, _ ->
                // Return to main passenger screen
                val intent = Intent(this, PassengerActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupButtons() {
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val confirmButton = findViewById<Button>(R.id.confirmButton)
        val feedbackButton = findViewById<Button>(R.id.feedbackButton)

        when (booking?.status) {
            "pending" -> {
                cancelButton.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { showCancelConfirmation() }
                }
                confirmButton.visibility = View.GONE
                feedbackButton.visibility = View.GONE
            }
            "accepted" -> {
                cancelButton.visibility = View.GONE
                confirmButton.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { showCompletionConfirmation() }
                }
                feedbackButton.visibility = View.GONE
            }
            "completed" -> {
                cancelButton.visibility = View.GONE
                confirmButton.visibility = View.GONE
                feedbackButton.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { showFeedbackDialog() }
                }
            }
            else -> {
                cancelButton.visibility = View.GONE
                confirmButton.visibility = View.GONE
                feedbackButton.visibility = View.GONE
            }
        }
    }

    private fun showFeedbackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText)

        AlertDialog.Builder(this)
            .setTitle("Rate Your Trip")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                submitFeedback(
                    rating = ratingBar.rating,
                    comment = commentEditText.text.toString()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitFeedback(rating: Float, comment: String) {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Submitting feedback...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        booking?.let { currentBooking ->
            val sharedPreferences = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
            val passengerId = sharedPreferences.getInt("user_id", 0)

            ApiClient.getApi(this).submitFeedback(
                bookingId = currentBooking.id,
                passengerId = passengerId,
                driverId = currentBooking.driver_id ?: 0,
                rating = rating,
                comment = comment
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        showFeedbackSuccess()
                    } else {
                        showError("Failed to submit feedback. Please try again.")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    showError("Network error: ${t.message}")
                }
            })
        }
    }

    private fun showFeedbackSuccess() {
        AlertDialog.Builder(this)
            .setTitle("Thank You!")
            .setMessage("Your feedback has been submitted successfully.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showCancelConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("Yes") { _, _ -> attemptToCancel() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun attemptToCancel() {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Processing cancellation...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        booking?.let { currentBooking ->
            val sharedPreferences = getSharedPreferences("login_pref", Context.MODE_PRIVATE)
            val passengerId = sharedPreferences.getInt("user_id", 0)

            ApiClient.getApi(this).cancelBooking(
                bookingId = currentBooking.id,
                passengerId = passengerId
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        showCancelSuccess()
                    } else {
                        val message = response.body()?.message ?: "Unable to cancel booking"
                        if (message.contains("already accepted", ignoreCase = true)) {
                            showDriverAcceptedDialog()
                        } else {
                            showError(message)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    showError("Network error: ${t.message}")
                }
            })
        }
    }

    private fun showCancelSuccess() {
        AlertDialog.Builder(this)
            .setTitle("Booking Cancelled")
            .setMessage("Your booking has been cancelled successfully.")
            .setPositiveButton("OK") { _, _ ->
                // Start PassengerActivity and clear the activity stack
                val intent = Intent(this, PassengerActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showCompletionConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Complete Trip")
            .setMessage("Are you sure you want to mark this trip as complete?")
            .setPositiveButton("Yes") { _, _ ->
                confirmTripCompletion()
            }
            .setNegativeButton("Not Yet", null)
            .show()
    }

    private fun showDriverAcceptedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cannot Cancel")
            .setMessage("This booking cannot be cancelled as a driver has already accepted it. Please refresh to see driver details.")
            .setPositiveButton("Refresh") { _, _ ->
                refreshBookingStatus()
            }
            .setNegativeButton("OK", null)
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

        Log.d("PassengerBooking", "Refreshing booking status for passenger: $passengerId")

        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Refreshing booking status...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        ApiClient.getApi(this).getPassengerActiveBooking(passengerId).enqueue(object : Callback<Booking?> {
            override fun onResponse(call: Call<Booking?>, response: Response<Booking?>) {
                loadingDialog.dismiss()
                if (response.isSuccessful) {
                    response.body()?.let { updatedBooking ->
                        Log.d("PassengerBooking", "Booking status: ${updatedBooking.status}")  // Add this log

                        val route = updatedBooking.route ?: "Unknown"
                        val dropoffLocation = updatedBooking.dropoff_location ?: route

                        // Show driver details message if booking is accepted
                        if (updatedBooking.status == "accepted") {
                            showDriverFoundMessage()
                        }
                        // Show feedback option if booking is completed
                        else if (updatedBooking.status == "completed") {
                            findViewById<Button>(R.id.feedbackButton).visibility = View.VISIBLE
                        }

                        booking = updatedBooking.copy(route = route, dropoff_location = dropoffLocation)
                        displayBookingDetails(booking!!)
                        setupButtons()
                    } ?: run {
                        Log.e("PassengerBooking", "Received null booking")
                    }
                } else {
                    Log.e("PassengerBooking", "Error response: ${response.code()}")
                    Log.e("PassengerBooking", "Error body: ${response.errorBody()?.string()}")
                    showError("Failed to refresh booking status")
                }
            }

            override fun onFailure(call: Call<Booking?>, t: Throwable) {
                loadingDialog.dismiss()
                Log.e("PassengerBooking", "Network error", t)
                showError("Failed to refresh: ${t.message}")
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