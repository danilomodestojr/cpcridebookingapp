package com.example.trikesafe

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    //private const val BASE_URL = "http://10.0.2.2/trikesafe-admin/" // localhost for Android emulator
    private const val BASE_URL = "http://192.168.254.108:80/trikesafe-admin/" // localhost for PC Xammp

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}