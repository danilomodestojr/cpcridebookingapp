import android.content.Context
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import com.example.trikesafe.ApiClient
import com.example.trikesafe.Booking
import com.example.trikesafe.CreateBookingResponse
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookingManager(
    private val context: Context,
    private val map: MapView,
    private var currentLocation: GeoPoint?
) {
    init {
        ApiClient.initialize(context)
    }
    fun updateCurrentLocation(location: GeoPoint?) {
        currentLocation = location
    }

    interface BookingCallback {
        fun onBookingSuccess(booking: Booking)
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

                            Log.d("BookingManager", "Tapped coords: lat=${tappedPoint?.latitude}, lon=${tappedPoint?.longitude}")

                            tappedPoint?.let { point ->
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

        Log.d("BookingManager", "Creating booking with passengerId: $passengerId")
        Log.d("BookingManager", "Pickup: ${currentLocation?.latitude}, ${currentLocation?.longitude}")
        Log.d("BookingManager", "Dropoff: ${destination.latitude}, ${destination.longitude}")

        ApiClient.getApi(context).createBooking(
            passengerId = passengerId,
            bookingType = "regular",
            pickupLocation = "Current Location",
            dropoffLocation = "Destination",
            pickupLatitude = currentLocation?.latitude ?: 0.0,
            pickupLongitude = currentLocation?.longitude ?: 0.0,
            dropoffLatitude = destination.latitude,
            dropoffLongitude = destination.longitude,
            distanceKm = distance,
            totalFare = fare
        ).enqueue(object : Callback<CreateBookingResponse> {
            override fun onResponse(
                call: Call<CreateBookingResponse>,
                response: Response<CreateBookingResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.booking?.let { booking ->
                        Log.d("BookingManager", "Booking created successfully: ${booking.id}")
                        callback.onBookingSuccess(booking)
                    } ?: run {
                        Log.e("BookingManager", "Booking object is null in response")
                        callback.onBookingError("Booking created but details not returned")
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Failed to create booking"
                    Log.e("BookingManager", "API Error: $errorMsg")
                    callback.onBookingError(errorMsg)
                }
            }

            override fun onFailure(call: Call<CreateBookingResponse>, t: Throwable) {
                Log.e("BookingManager", "Network error: ${t.message}", t)
                callback.onBookingError("Network error: ${t.message}")
            }
        })
    }
}