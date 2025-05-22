package com.example.md_47_googlemaps

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
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
import androidx.lifecycle.viewModelScope
import com.example.md_47_googlemaps.extensions.asString
import com.example.md_47_googlemaps.network.OsrmApi
import com.example.md_47_googlemaps.network.RetrofitProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private val Context.dataStore: DataStore<Preferences>
            by preferencesDataStore(name = "map_settings")
    private var selectedPoints = mutableListOf<LatLng>()

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _isRouteReady = MutableLiveData<Boolean>(false)
    val isRouteReady: LiveData<Boolean> = _isRouteReady

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

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
        getApplication<Application>().dataStore.edit {
            it[LAST_LONG] = googleMap.cameraPosition.target.longitude
            it[LAST_LAT] = googleMap.cameraPosition.target.latitude
            it[LAST_ZOOM] = googleMap.cameraPosition.zoom
        }
    }

    suspend fun loadPosition() {
        getApplication<Application>().dataStore.edit {
            val longitude = it[LAST_LONG]
            val latitude = it[LAST_LAT]
            val zoom = it[LAST_ZOOM]

            longitude?.let {
                moveToPoint(it, latitude!!, zoom!!)
            }
        }
    }

    fun toMyLocation() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(
                    getApplication<Application>(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    getApplication<Application>(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("Locator", "Permissions not granted")

                return
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    Log.d("Locator", "Locate to position")
                    val myLocation = LatLng(it.latitude, it.longitude)
                    moveToPoint(myLocation, 17f)
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
        viewModelScope.launch {
            try {
                _isLoading.value = true;
                val response = withContext(Dispatchers.IO) {
                    apiObject.getRoute(
                        start.asString(),
                        end.asString()
                    )
                }
                if(response.isSuccessful){
                    response.body()?.let {
                        if(it.routes[0].distance == 0f){
                            _toastMessage.value = "Маршрут не найден";
                            clearPath()
                            return@let
                        }

                        val polyline = it.routes[0].geometry
                        val decodedPolyline = PolyUtil.decode(polyline)
                        withContext(Dispatchers.Main) {
                            drawPolyline(decodedPolyline)
                            _isRouteReady.value = true
                            _toastMessage.value = "Маршрут построен"
                        }
                    }
                }
                else{
                    _toastMessage.value = "Маршрут не найден";
                    clearPath()
                }

            } catch (e: IOException) {
                _toastMessage.value = "Ошибка сети: ${e.message}"
                Log.e("OsrmNetwork", "Network error", e)
                clearPath()
            } catch(e: Exception) {
                _toastMessage.value = "Непредвиденная ошибка"
                Log.e("OsrmException", "Unexpected", e)
            } finally {
                _isLoading.value = false
            }

        }
    }

    fun drawPolyline(decodedPolyline : List<LatLng>){
        googleMap.addPolyline(
            PolylineOptions()
                .addAll(decodedPolyline)
                .color(Color.RED)
                .width(15f)
        )
    }

    fun clearPath() {
        selectedPoints.clear()
        googleMap.clear()
        _isRouteReady.value = false
    }

    fun initGoogle(p0: GoogleMap) {
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

class MapViewModelFactory(val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(app) as T
    }
}