package com.example.trikesafe

import java.io.Serializable

data class Booking(
    val id: Int,
    val passenger_id: Int,
    val pickup_location: String,
    val dropoff_location: String,
    val pickup_latitude: Double,
    val pickup_longitude: Double,
    val dropoff_latitude: Double,
    val dropoff_longitude: Double,
    val distance_km: Double,
    val total_fare: Double,
    val status: String
) : Serializable