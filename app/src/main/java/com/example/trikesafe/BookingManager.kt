import android.content.Context
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.io.IOException

class BookingManager(
    private val context: Context,
    private val map: MapView,
    private var currentLocation: GeoPoint?
) {

    private val client = OkHttpClient()
    private val url = "http://192.168.254.108:80/trikesafe-admin/bookings.php"

    fun updateCurrentLocation(location: GeoPoint?) {
        currentLocation = location
    }

    interface BookingCallback {
        fun onBookingSuccess()
        fun onBookingError(message: String)
    }

    fun showBookingOptions(callback: BookingCallback) {
        if (currentLocation == null) {
            showPickupLocationDialog()
            return
        }
        showRideTypeDialog(callback)
    }

    private fun showPickupLocationDialog() {
        AlertDialog.Builder(context)
            .setMessage("Please set your pickup location first")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showRideTypeDialog(callback: BookingCallback) {
        AlertDialog.Builder(context)
            .setTitle("Choose Ride Type")
            .setItems(arrayOf("Point-to-Point Ride", "City Tour Package")) { _, which ->
                when (which) {
                    0 -> handlePointToPointBooking(callback)
                    1 -> handleCityTourBooking()
                }
            }
            .show()
    }

    private fun addMarker(point: GeoPoint) {
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun handlePointToPointBooking(callback: BookingCallback) {
        AlertDialog.Builder(context)
            .setTitle("Set Destination")
            .setMessage("Please tap your destination on the map")
            .setPositiveButton("OK") { _, _ ->
                map.overlays.add(object : Overlay() {
                    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                        e?.let {
                            val tappedPoint = mapView?.projection?.fromPixels(e.x.toInt(), e.y.toInt())

                            // Log raw tapped coordinates
                            Log.d("BookingManager", "Tapped coords: lat=${tappedPoint?.latitude}, lon=${tappedPoint?.longitude}")

                            tappedPoint?.let { point ->
                                // Use exact coordinates without modification
                                val destPoint = GeoPoint(point.latitude, point.longitude)
                                Log.d("BookingManager", "Creating destination GeoPoint: lat=${destPoint.latitude}, lon=${destPoint.longitude}")

                                map.overlays.clear()
                                addMarker(currentLocation!!)
                                addDestinationMarker(destPoint)
                                calculateFare(destPoint, callback)
                            }
                        }
                        return true
                    }
                })
            }
            .show()
    }

    private fun handleCityTourBooking() {
        // Implementation for city tour
    }

    private fun addDestinationMarker(point: GeoPoint) {
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = context.resources.getDrawable(android.R.drawable.ic_menu_myplaces)
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun calculateFare(destinationPoint: GeoPoint, callback: BookingCallback) {
        val distance = currentLocation?.distanceToAsDouble(destinationPoint)?.div(1000) ?: 0.0
        val baseFare = 30.0
        val perKmRate = 10.0
        val totalFare = baseFare + (distance * perKmRate)

        showBookingConfirmation(destinationPoint, distance, totalFare, callback)
    }

    private fun showBookingConfirmation(
        destination: GeoPoint,
        distance: Double,
        fare: Double,
        callback: BookingCallback
    ) {
        AlertDialog.Builder(context)
            .setTitle("Booking Details")
            .setMessage("""
                Distance: ${String.format("%.2f", distance)} km
                Base Fare: ₱30.00
                Total Fare: ₱${String.format("%.2f", fare)}
                
                Would you like to confirm booking?
            """.trimIndent())
            .setPositiveButton("Confirm") { _, _ -> saveBooking(destination, distance, fare, callback) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBooking(
        destination: GeoPoint,
        distance: Double,
        fare: Double,
        callback: BookingCallback
    ) {
        val sharedPreferences = context.getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val passengerId = sharedPreferences.getInt("user_id", 0)

        // Log coordinates before creating FormBody
        Log.d("BookingManager", "Saving booking: pickup_lat=${currentLocation?.latitude}, pickup_long=${currentLocation?.longitude}")
        Log.d("BookingManager", "Saving booking: dropoff_lat=${destination.latitude}, dropoff_long=${destination.longitude}")

        val formBody = FormBody.Builder()
            .add("passenger_id", passengerId.toString())
            .add("booking_type", "regular")
            .add("pickup_location", "Current Location")
            .add("dropoff_location", "Destination")
            .add("pickup_latitude", currentLocation?.latitude?.toString() ?: "0.0")
            .add("pickup_longitude", currentLocation?.longitude?.toString() ?: "0.0")
            .add("dropoff_latitude", destination.latitude.toString())
            .add("dropoff_longitude", destination.longitude.toString())
            .add("distance_km", distance.toString())
            .add("total_fare", fare.toString())
            .build()

        // Log the actual form data being sent
        Log.d("BookingManager", "Form data: ${formBody.toString()}")

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onBookingError("Failed to save booking")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("BookingManager", "Response: $responseBody")
                callback.onBookingSuccess()
            }
        })
    }
}