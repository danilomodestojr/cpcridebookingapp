package com.example.trikesafe

data class TourPackagesResponse(
    val success: Boolean,
    val packages: List<TourPackage>?  // ✅ Ensure this correctly references `TourPackage`
)
