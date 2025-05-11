package com.example.md_47_googlemaps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.md_47_googlemaps.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var googleMap : GoogleMap
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

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

    fun initGoogleObjects(){
        val mapFragment : SupportMapFragment = supportFragmentManager
            .findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    fun initUI(){
        binding.postOfficeBtn.setOnClickListener{
            moveToPostOffice()
        }

        binding.myLocButton.setOnClickListener{
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION,
                ::toMyLocation)
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
    }

    fun checkPermission(permission: String, void: () -> Unit){
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

    fun moveToPostOffice(){
        val postOfficeCoords = LatLng(55.354993, 86.085805)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(postOfficeCoords, 17f))
    }

    fun toMyLocation(){
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
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.d("MyLocation", "Doing stuff")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val myLocation = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17f))
                }
            }
        }
        else {
            Toast.makeText(this, "Включите GPS", Toast.LENGTH_LONG).show()
        }

    }
}