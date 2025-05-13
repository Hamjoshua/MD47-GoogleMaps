package com.example.md_47_googlemaps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.md_47_googlemaps.databinding.ActivityMainBinding
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

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private var selectedPoints = mutableListOf<LatLng>()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Разрешение получено

            } else {

            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initGoogleObjects()
        initUI()
    }

    fun initGoogleObjects() {
        val mapFragment: SupportMapFragment = supportFragmentManager
            .findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    fun initUI() {
        binding.postOfficeBtn.setOnClickListener {
            moveToPostOffice()
        }

        binding.myLocButton.setOnClickListener {
            checkPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                ::toMyLocation
            )
        }

        binding.clearPathBtn.setOnClickListener{
            clearPath()
        }
        binding.clearPathBtn.isVisible = false
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        googleMap.setOnMapLongClickListener { latLong ->
            onLongTap(latLong)
        }
    }

    fun checkPermission(permission: String, void: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                void()

            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, permission
            ) -> {
                // Показываем объяснение перед запросом
                requestPermissionLauncher.launch(permission)
            }

            else -> {
                // Просто запрашиваем разрешение
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    fun moveToPostOffice() {
        val postOfficeCoords = LatLng(55.354993, 86.085805)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(postOfficeCoords, 17f))
    }

    fun onLongTap(latLng: LatLng) {
        if (selectedPoints.size < 2) {
            selectedPoints.add(latLng)
            googleMap.addMarker(MarkerOptions().position(latLng))

            // Если выбрано 2 точки — строим маршрут
            if (selectedPoints.size == 2) {
                drawRoute(selectedPoints[0], selectedPoints[1])
                binding.clearPathBtn.isVisible = true
            }
        }
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        val apiObject = RetrofitProvider.getInstance()
            .create(OsrmApi::class.java)
        GlobalScope.launch(Dispatchers.IO) {
            val response = apiObject.getRoute(
                start.asString(),
                end.asString()
            )
            Log.d("OsrmResponse", response.body().toString());
            Log.d("OsrmReq", response.raw().toString());
            response.body()?.let {
                val polyline = it.routes[0].geometry
                val decodedPolyline = PolyUtil.decode(polyline)

                runOnUiThread {
                    googleMap.addPolyline(
                        PolylineOptions()
                            .addAll(decodedPolyline)
                            .color(Color.RED)
                            .width(15f)
                    )
                }
            }
        }
    }

    fun clearPath(){
        selectedPoints.clear()
        googleMap.clear()
        binding.clearPathBtn.isVisible = false;
    }

    fun toMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("MyLocation", "Doing stuff")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val myLocation = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17f))
                }
            }
        } else {
            Toast.makeText(this, "Включите GPS", Toast.LENGTH_LONG).show()
        }

    }
}