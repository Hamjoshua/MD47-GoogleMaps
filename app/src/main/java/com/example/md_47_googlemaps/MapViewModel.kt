package com.example.md_47_googlemaps

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.md_47_googlemaps.extensions.asString
import com.example.md_47_googlemaps.network.OsrmApi
import com.example.md_47_googlemaps.network.RetrofitProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapViewModel(val application: Application) : AndroidViewModel(application),
    OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private val Context.dataStore: DataStore<Preferences>
            by preferencesDataStore(name = "map_settings")
    private var selectedPoints = mutableListOf<LatLng>()

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    companion object DataStoreKeys {
        val LAST_LONG = doublePreferencesKey("last_longitude") // долгота
        val LAST_LAT = doublePreferencesKey("last_latitude") // широта
        val LAST_ZOOM = floatPreferencesKey("last_zoom")
    }

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    suspend fun setPosition() {
        application.dataStore.edit {
            it[LAST_LONG] = googleMap.cameraPosition.target.longitude
            it[LAST_LAT] = googleMap.cameraPosition.target.latitude
            it[LAST_ZOOM] = googleMap.cameraPosition.zoom
        }
    }

    suspend fun loadPosition() {
        application.dataStore.edit {
            val longitude = it[LAST_LONG]
            val latitude = it[LAST_LAT]
            val zoom = it[LAST_ZOOM]

            moveToPoint(longitude!!, latitude!!, zoom!!)
        }
    }

    fun toMyLocation(){
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("MyLocation", "Doing stuff")
            if (ActivityCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val myLocation = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17f))
                }
            }
        } else {
            _toastMessage.value = "Включите GPS"
        }
    }

    fun moveToPostOffice() {
        val postOfficeCoords = LatLng(55.354993, 86.085805)
        moveToPoint(postOfficeCoords, 17f)
    }

    fun moveToPoint(longitude: Double, latitude: Double, zoom: Float) {
        val coords = LatLng(latitude, longitude)
        moveToPoint(coords, zoom)
    }

    fun moveToPoint(coords: LatLng, zoom: Float) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coords, zoom))
    }

    fun onLongTap(latLng: LatLng) {
        if (selectedPoints.size < 2) {
            selectedPoints.add(latLng)
            googleMap.addMarker(MarkerOptions().position(latLng))

            // Если выбрано 2 точки — строим маршрут
            if (selectedPoints.size == 2) {
                drawRoute(selectedPoints[0], selectedPoints[1])
            }
        }
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        val apiObject = RetrofitProvider.getInstance()
            .create(OsrmApi::class.java)
        viewModelScope.launch(Dispatchers.IO) {
            val response = apiObject.getRoute(
                start.asString(),
                end.asString()
            )
            Log.d("OsrmResponse", response.body().toString());
            Log.d("OsrmReq", response.raw().toString());
            response.body()?.let {
                val polyline = it.routes[0].geometry
                val decodedPolyline = PolyUtil.decode(polyline)

                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(decodedPolyline)
                        .color(Color.RED)
                        .width(15f)
                )

            }
        }
    }

    fun clearPath() {
        selectedPoints.clear()
        googleMap.clear()
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        viewModelScope.launch {
            loadPosition()
            return@launch
        }

        googleMap.setOnMapLongClickListener { latLong ->
            onLongTap(latLong)
        }

        googleMap.setOnCameraIdleListener {
            viewModelScope.launch {
                setPosition()
            }
        }
    }
}

class MapViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(app) as T
    }
}