package com.example.md_47_googlemaps.dataClasses

data class OsrmResponse(
    val routes: ArrayList<OsrmRoute>
)

data class OsrmRoute(
    val geometry: String,
    val weight: Float,
    val duration: Float,
    val distance: Float
)
