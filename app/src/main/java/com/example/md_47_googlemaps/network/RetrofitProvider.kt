package com.example.md_47_googlemaps.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    private const val baseUrl = "https://router.project-osrm.org/"
    private const val timeout = 30L
    fun getInstance(): Retrofit {
        val okClient = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}