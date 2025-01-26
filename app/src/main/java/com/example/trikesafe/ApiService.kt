package com.example.trikesafe

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("register.php")
    suspend fun registerUser(@Body user: UserData): Response<ApiResponse>

    @POST("login_process.php")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<ApiResponse>
}

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val userId: Int?,
    val role: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)