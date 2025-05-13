package com.example.md_47_googlemaps.extensions

import com.google.android.gms.maps.model.LatLng

fun LatLng.asString() : String {
    return "${latitude},${longitude}"
}