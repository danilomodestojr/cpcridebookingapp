package com.example.trikesafe

import java.io.Serializable

data class Booking(
    val id: Int,
    val passenger_id: Int,
    val driver_id: Int?,  // Added as nullable since it's null when booking is pending
    val pickup_location: String,
    val dropoff_location: String,
    val pickup_latitude: Double,
    val pickup_longitude: Double,
    val dropoff_latitude: Double,
    val dropoff_longitude: Double,
    val distance_km: Double,
    val total_fare: Double,
    val status: String,
    val passenger_name: String?,  // Added for passenger info
    val driver_name: String?,     // Added for driver info
    val passenger_contact: String?,  // Added for passenger's contact number
    val driver_contact: String?,
    val booking_type: String = "regular",
    val tour_package_id: Int? = null,
    val tour_name: String? = null,
    val tour_points: String? = null
) : Serializable