package com.example.trikesafe

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private var retrofit: Retrofit? = null
    private var api: ApiService? = null

    fun initialize(context: Context) {
        val sharedPreferences = context.getSharedPreferences("server_config", Context.MODE_PRIVATE)
        val ipAddress = sharedPreferences.getString("ip_address", "192.168.254.108")
        val port = sharedPreferences.getString("port", "80")

        val baseUrl = "http://$ipAddress:$port/trikesafe-admin/"

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit?.create(ApiService::class.java)
    }

    fun getApi(context: Context): ApiService {
        if (api == null) {
            initialize(context)
        }
        return api ?: throw IllegalStateException("API not initialized")
    }
}