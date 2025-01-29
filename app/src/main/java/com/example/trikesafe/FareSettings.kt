// FareSettings.kt
package com.example.trikesafe  // Update this to match your package name

data class FareSettings(
    val base_fare: Double,
    val additional_per_km: Double,
    val minimum_distance: Double
)

data class FareSettingsResponse(
    val success: Boolean,
    val settings: FareSettings
)