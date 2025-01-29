package com.example.trikesafe

data class TourPackage(
    val id: Int,
    val name: String,
    val description: String,
    val route_Points: String,  // ✅ Changed from `route_points` to `routePoints`
    val duration_Minutes: Int,  // ✅ Changed from `duration_minutes` to `durationMinutes`
    val price: Double,
    val dropoffLatitude: Double,
    val dropoffLongitude: Double
)
