package com.example.trikesafe

import BookingManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import android.Manifest
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

class PassengerActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var bookingManager: BookingManager
    private val requestPermissionsCode = 1
    private var currentLocation: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", Context.MODE_PRIVATE))
        setContentView(R.layout.activity_passenger)

        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(19.0)
        val startPoint = GeoPoint(11.5830094, 122.7530419)
        Log.d("PassengerActivity", "Setting map center: lat=${startPoint.latitude}, lon=${startPoint.longitude}")
        mapController.setCenter(startPoint)

        bookingManager = BookingManager(this, map, currentLocation)
        showLocationSelectionDialog()

        requestPermissionsIfNecessary(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        setupButtons()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            getSharedPreferences("login_pref", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<FloatingActionButton>(R.id.bookRideButton).setOnClickListener {
            bookingManager.showBookingOptions(object : BookingManager.BookingCallback {
                override fun onBookingSuccess() {
                    runOnUiThread { showSuccess("Booking saved successfully!") }
                }

                override fun onBookingError(message: String) {
                    runOnUiThread { showError(message) }
                }
            })
        }

        findViewById<Button>(R.id.changeLocationButton).setOnClickListener {
            showLocationSelectionDialog()
        }
    }

    private fun showLocationSelectionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Set Your Location")
            .setMessage("Map is centered at Roxas City. Please tap your current location for pickup.")
            .setPositiveButton("OK") { dialog, _ ->
                map.overlays.add(object : Overlay() {
                    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                        e?.let {
                            val tappedPoint = mapView?.projection?.fromPixels(e.x.toInt(), e.y.toInt())

                            // Log raw tapped coordinates
                            Log.d("PassengerActivity", "Tapped coords: lat=${tappedPoint?.latitude}, lon=${tappedPoint?.longitude}")

                            tappedPoint?.let { point ->
                                map.overlays.clear()
                                // Use exact coordinates without modification
                                val geoPoint = GeoPoint(point.latitude, point.longitude)
                                Log.d("PassengerActivity", "Creating GeoPoint: lat=${geoPoint.latitude}, lon=${geoPoint.longitude}")

                                addMarker(geoPoint)
                                currentLocation = geoPoint
                                bookingManager.updateCurrentLocation(currentLocation)

                                // Log final stored location
                                Log.d("PassengerActivity", "Stored location: lat=${currentLocation?.latitude}, lon=${currentLocation?.longitude}")
                            }
                        }
                        return true
                    }
                })
                dialog.dismiss()
            }
            .show()
    }

    private fun addMarker(point: GeoPoint) {
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()

        AlertDialog.Builder(this)
            .setMessage("Location set! You can now book a ride using the button below.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccess(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK", null)
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

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, requestPermissionsCode)
        }
    }
}