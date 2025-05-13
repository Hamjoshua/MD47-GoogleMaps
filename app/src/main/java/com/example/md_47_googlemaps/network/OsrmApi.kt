package com.example.md_47_googlemaps.network

import com.example.md_47_googlemaps.dataClasses.OsrmResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OsrmApi {
    @GET("route/v1/driving/{start};{end}")
    suspend fun getRoute(
        @Path("start") start: String,
        @Path("start") end: String,
        @Query("overview") overview: String = "full"
    ) : Response<OsrmResponse>
}