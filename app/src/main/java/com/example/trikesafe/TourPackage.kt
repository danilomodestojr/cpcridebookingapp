package com.example.trikesafe

data class TourPackage(
    val id: Int,
    val name: String,
    val description: String,
    val route_points: String,
    val duration_minutes: Int,
    val price: Double,
    val created_at: String
)

data class TourPackagesResponse(
    val success: Boolean,
    val packages: List<TourPackage>,
    val message: String? = null
)