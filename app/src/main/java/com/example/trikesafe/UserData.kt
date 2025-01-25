package com.example.trikesafe

data class UserData(
    val username: String,
    val name: String,       // maps to 'first_name' in the DB
    val last_name: String,
    val phone: String,      // maps to 'contact_number'
    val email: String,
    val password: String,
    val role: String,
    val driver_license: String? = null,
    val tricycle_number: String? = null
)
