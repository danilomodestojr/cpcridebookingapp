package com.example.trikesafe

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class BookingDetailActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private var booking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

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

        // Setup button
        findViewById<Button>(R.id.acceptButton).setOnClickListener {
            showAcceptConfirmation()
        }
    }

    private fun displayBookingDetails(booking: Booking) {
        findViewById<TextView>(R.id.pickupLocationText).text =
            "Pickup Location: ${booking.pickup_location}"
        findViewById<TextView>(R.id.dropoffLocationText).text =
            "Dropoff Location: ${booking.dropoff_location}"
        findViewById<TextView>(R.id.distanceText).text =
            "Distance: ${String.format("%.2f", booking.distance_km)} km"
        findViewById<TextView>(R.id.fareText).text =
            "Fare: ₱${String.format("%.2f", booking.total_fare)}"
    }

    private fun setupMap(booking: Booking) {
        // Set user agent to prevent tile loading issues
        Configuration.getInstance().userAgentValue = packageName

        val pickupPoint = GeoPoint(booking.pickup_latitude, booking.pickup_longitude)
        val dropoffPoint = GeoPoint(booking.dropoff_latitude, booking.dropoff_longitude)

        // Clear existing overlays
        map.overlays.clear()

        // Add pickup marker
        val pickupMarker = Marker(map).apply {
            position = pickupPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Pickup Location"
            icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation)
        }
        map.overlays.add(pickupMarker)

        // Add dropoff marker
        val dropoffMarker = Marker(map).apply {
            position = dropoffPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Dropoff Location"
            icon = resources.getDrawable(android.R.drawable.ic_menu_myplaces)
        }
        map.overlays.add(dropoffMarker)

        // Configure map
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(16.0)

        // Center map to show both points
        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(listOf(pickupPoint, dropoffPoint))
        map.zoomToBoundingBox(boundingBox, true)

        // Force map update
        map.invalidate()
    }

    private fun showAcceptConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Accept Booking")
            .setMessage("Are you sure you want to accept this booking?")
            .setPositiveButton("Accept") { _, _ ->
                // We'll implement booking acceptance next
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
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