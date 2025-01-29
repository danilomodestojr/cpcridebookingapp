import android.content.Context
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import com.example.trikesafe.ApiClient
import com.example.trikesafe.Booking
import com.example.trikesafe.CreateBookingResponse
import com.example.trikesafe.FareSettings
import com.example.trikesafe.FareSettingsResponse
import com.example.trikesafe.TourPackage
import com.example.trikesafe.TourPackagesResponse  // ✅ Ensure this is correctly imported
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
    private var fareSettings: FareSettings? = null

    init {
        ApiClient.initialize(context)
        loadFareSettings()
    }

    private fun loadFareSettings() {
        ApiClient.getApi(context).getFareSettings().enqueue(object : Callback<FareSettingsResponse> {
            override fun onResponse(call: Call<FareSettingsResponse>, response: Response<FareSettingsResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    fareSettings = response.body()?.settings
                } else {
                    Log.e("BookingManager", "Failed to load fare settings: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<FareSettingsResponse>, t: Throwable) {
                Log.e("BookingManager", "Failed to load fare settings: ${t.message}")
            }
        })
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
                    1 -> handleCityTourBooking(callback)  // ✅ Fix: Pass callback correctly
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
        if (fareSettings == null) {
            AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("Unable to get fare information. Please try again later.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

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

    private fun handleCityTourBooking(callback: BookingCallback) {
        if (currentLocation == null) {
            showPickupLocationDialog()
            return
        }

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(context)
            .setMessage("Loading tour packages...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Fetch tour packages
        ApiClient.getApi(context).getTourPackages().enqueue(object : Callback<TourPackagesResponse> {
            override fun onResponse(
                call: Call<TourPackagesResponse>,
                response: Response<TourPackagesResponse>
            ) {
                loadingDialog.dismiss()
                if (response.isSuccessful) {
                    val tourResponse = response.body()
                    if (tourResponse?.success == true) {
                        tourResponse.packages?.let { packages ->
                            showTourPackagesList(packages, callback)
                        } ?: run {
                            showError("No tour packages available")
                        }
                    } else {
                        showError("Failed to load tour packages")
                    }
                } else {
                    Log.e("BookingManager", "Error response code: ${response.code()}")
                    Log.e("BookingManager", "Error body: ${response.errorBody()?.string()}")
                    showError("Failed to load tour packages")
                }
            }

            override fun onFailure(call: Call<TourPackagesResponse>, t: Throwable) {
                loadingDialog.dismiss()
                showError("Network error: ${t.message}")
            }
        })
    }


    private fun showTourPackagesList(packages: List<TourPackage>, callback: BookingCallback) {
        if (packages.isEmpty()) {
            showError("No tour packages available")
            return
        }

        // Create package names array for dialog
        val packageNames = packages.map { it.name }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Available Tour Packages")
            .setItems(packageNames) { _, which ->
                val selectedPackage = packages[which]
                showTourPackageDetails(selectedPackage, callback)  // ✅ Fix: Pass callback correctly
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showTourPackageDetails(tourPackage: TourPackage, callback: BookingCallback) {
        val durationHours = tourPackage.duration_Minutes / 60.0
        val message = """
        Package: ${tourPackage.name}
        
        Description: ${tourPackage.description}
        
        Places to Visit: ${tourPackage.route_Points}
        
        Duration: ${String.format("%.1f", durationHours)} hours
        Price: ₱${String.format("%.2f", tourPackage.price)}
        
        Would you like to book this tour package?
    """.trimIndent()

        AlertDialog.Builder(context)
            .setTitle("Tour Package Details")
            .setMessage(message)
            .setPositiveButton("Book Now") { _, _ ->
                createTourBooking(tourPackage, callback)  // Now passing the callback
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun createTourBooking(tourPackage: TourPackage, callback: BookingCallback) {
        val sharedPreferences = context.getSharedPreferences("login_pref", Context.MODE_PRIVATE)
        val passengerId = sharedPreferences.getInt("user_id", 0)

        // ✅ Ensure we fetch dropoff latitude & longitude from tourPackage
        val dropoffLat = tourPackage.dropoffLatitude
        val dropoffLon = tourPackage.dropoffLongitude

        Log.d("BookingManager", "Tour Package Dropoff Coords: lat=$dropoffLat, lon=$dropoffLon")

        ApiClient.getApi(context).createBooking(
            passengerId = passengerId,
            bookingType = "tour",
            pickupLocation = "Tour Pickup Point",
            dropoffLocation = tourPackage.route_Points,
            pickupLatitude = currentLocation?.latitude ?: 0.0,
            pickupLongitude = currentLocation?.longitude ?: 0.0,
            dropoffLatitude = dropoffLat,  // ✅ Correctly fetching from `tourPackage`
            dropoffLongitude = dropoffLon,  // ✅ Correctly fetching from `tourPackage`
            distanceKm = 0.0,
            totalFare = tourPackage.price,
            tourPackageId = tourPackage.id
        ).enqueue(object : Callback<CreateBookingResponse> {
            override fun onResponse(call: Call<CreateBookingResponse>, response: Response<CreateBookingResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.booking?.let { booking ->
                        Log.d("BookingManager", "Tour booking created successfully: ${booking.id}")
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






    private fun showError(message: String) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
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
        // If fare settings aren't loaded yet, show error
        if (fareSettings == null) {
            callback.onBookingError("Unable to calculate fare. Please try again.")
            return
        }

        val distance = currentLocation?.distanceToAsDouble(destinationPoint)?.div(1000) ?: 0.0

        val totalFare = if (distance <= fareSettings!!.minimum_distance) {
            // If distance is within minimum distance (4km), just charge base fare
            fareSettings!!.base_fare
        } else {
            // If distance exceeds 4km, add charges for additional distance
            val additionalDistance = distance - fareSettings!!.minimum_distance
            fareSettings!!.base_fare + (additionalDistance * fareSettings!!.additional_per_km)
        }

        showBookingConfirmation(destinationPoint, distance, totalFare, callback)
    }

    private fun showBookingConfirmation(
        destination: GeoPoint,
        distance: Double,
        fare: Double,
        callback: BookingCallback
    ) {
        val message = if (distance <= fareSettings!!.minimum_distance) {
            """
        Distance: ${String.format("%.2f", distance)} km
        Base Fare (up to ${fareSettings!!.minimum_distance}km): ₱${String.format("%.2f", fareSettings!!.base_fare)}
        Total Fare: ₱${String.format("%.2f", fare)}
        
        Would you like to confirm booking?
        """.trimIndent()
        } else {
            val additionalDistance = distance - fareSettings!!.minimum_distance
            """
        Distance: ${String.format("%.2f", distance)} km
        Base Fare (first ${fareSettings!!.minimum_distance}km): ₱${String.format("%.2f", fareSettings!!.base_fare)}
        Additional Distance: ${String.format("%.2f", additionalDistance)} km
        Additional Charge: ₱${String.format("%.2f", additionalDistance * fareSettings!!.additional_per_km)}
        Total Fare: ₱${String.format("%.2f", fare)}
        
        Would you like to confirm booking?
        """.trimIndent()
        }

        AlertDialog.Builder(context)
            .setTitle("Booking Details")
            .setMessage(message)
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