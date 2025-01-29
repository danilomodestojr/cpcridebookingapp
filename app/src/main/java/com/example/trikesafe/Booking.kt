package com.example.trikesafe

import java.io.Serializable

data class Booking(
    val id: Int,
    val passenger_id: Int,
    val driver_id: Int?,
    val booking_type: String,
    val pickup_location: String,
    val dropoff_location: String?,  // ✅ Use snake_case as in database
    val pickup_latitude: Double,
    val pickup_longitude: Double,
    val dropoff_latitude: Double,
    val dropoff_longitude: Double,
    val distance_km: Double,
    val base_fare: Double?,
    val total_fare: Double,
    val status: String,
    val created_at: String,
    val tour_package_id: Int?,
    val passenger_name: String?,
    val passenger_contact: String?,
    val driver_name: String?,
    val driver_contact: String?,
    val tour_name: String?,
    val tour_description: String?,
    val route: String?,  // ✅ Ensure `route` is also included
    val tour_points: String?
) : Serializable