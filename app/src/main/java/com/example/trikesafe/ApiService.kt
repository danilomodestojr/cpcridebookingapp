package com.example.trikesafe

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

import com.example.trikesafe.TourPackagesResponse  // ✅ Corrected Import
import com.example.trikesafe.TourPackage  // ✅ Ensure TourPackage is also imported

interface ApiService {
    @POST("register.php")
    suspend fun registerUser(@Body user: UserData): Response<ApiResponse>

    @POST("login_process.php")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<ApiResponse>

    @GET("bookings.php?status=pending")
    fun getPendingBookings(): Call<List<Booking>>


    @GET("bookings.php")
    fun getFareSettings(@Query("action") action: String = "get_fare_settings"): Call<FareSettingsResponse>

    @GET("bookings.php")
    fun getDriverActiveBooking(
        @Query("driver_id") driverId: Int,
        @Query("booking_id") bookingId: Int,  // Add this parameter
        @Query("action") action: String = "check_active"
    ): Call<Booking?>

    // ✅ Fixed: Removed duplicate `getTourPackages`
    @GET("bookings.php")
    fun getTourPackages(@Query("action") action: String = "get_tour_packages"): Call<TourPackagesResponse>

    // Driver-related endpoints
    @FormUrlEncoded
    @POST("bookings.php")
    fun acceptBooking(
        @Field("action") action: String = "accept",
        @Field("booking_id") bookingId: Int,
        @Field("driver_id") driverId: Int
    ): Call<ApiResponse>

    @GET("bookings.php")
    fun getDriverActiveBooking(
        @Query("driver_id") driverId: Int,
        @Query("action") action: String = "check_active"
    ): Call<Booking?>

    @FormUrlEncoded
    @POST("bookings.php")
    fun markBookingComplete(
        @Field("action") action: String = "driver_complete",
        @Field("booking_id") bookingId: Int,
        @Field("driver_id") driverId: Int
    ): Call<ApiResponse>

    // Passenger-related endpoints
    @GET("bookings.php")
    fun getPassengerActiveBooking(
        @Query("passenger_id") passengerId: Int,
        @Query("action") action: String = "check_passenger_active"
    ): Call<Booking?>

    @FormUrlEncoded
    @POST("bookings.php")
    fun confirmBookingComplete(
        @Field("action") action: String = "passenger_confirm",
        @Field("booking_id") bookingId: Int,
        @Field("passenger_id") passengerId: Int
    ): Call<ApiResponse>

    // ✅ Ensure `dropoff_latitude` and `dropoff_longitude` are properly included
    @FormUrlEncoded
    @POST("bookings.php")
    fun createBooking(
        @Field("passenger_id") passengerId: Int,
        @Field("booking_type") bookingType: String,
        @Field("pickup_location") pickupLocation: String,
        @Field("dropoff_location") dropoffLocation: String,
        @Field("pickup_latitude") pickupLatitude: Double,
        @Field("pickup_longitude") pickupLongitude: Double,
        @Field("dropoff_latitude") dropoffLatitude: Double,  // ✅ Added correct parameter
        @Field("dropoff_longitude") dropoffLongitude: Double, // ✅ Added correct parameter
        @Field("distance_km") distanceKm: Double,
        @Field("total_fare") totalFare: Double,
        @Field("tour_package_id") tourPackageId: Int? = null  // ✅ Ensure nullable for non-tour bookings
    ): Call<CreateBookingResponse>
}

// Response data classes
data class ApiResponse(
    val success: Boolean,
    val message: String,
    val userId: Int?,
    val role: String? = null
)

data class CreateBookingResponse(
    val success: Boolean,
    val message: String,
    val booking: Booking?
)

data class LoginRequest(
    val username: String,
    val password: String
)
