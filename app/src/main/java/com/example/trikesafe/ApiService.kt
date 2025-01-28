package com.example.trikesafe

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("register.php")
    suspend fun registerUser(@Body user: UserData): Response<ApiResponse>

    @POST("login_process.php")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<ApiResponse>

    @GET("bookings.php?status=pending")
    fun getPendingBookings(): Call<List<Booking>>

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

    // Create new booking endpoint
    @FormUrlEncoded
    @POST("bookings.php")
    fun createBooking(
        @Field("passenger_id") passengerId: Int,
        @Field("booking_type") bookingType: String,
        @Field("pickup_location") pickupLocation: String,
        @Field("dropoff_location") dropoffLocation: String,
        @Field("pickup_latitude") pickupLatitude: Double,
        @Field("pickup_longitude") pickupLongitude: Double,
        @Field("dropoff_latitude") dropoffLatitude: Double,
        @Field("dropoff_longitude") dropoffLongitude: Double,
        @Field("distance_km") distanceKm: Double,
        @Field("total_fare") totalFare: Double
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