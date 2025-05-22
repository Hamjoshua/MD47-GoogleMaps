package com.example.md_47_googlemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.md_47_googlemaps.databinding.FragmentMapBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MapFragment : Fragment(), OnMapReadyCallback {
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
    private lateinit var binding: FragmentMapBinding
    val viewModel: MapViewModel by viewModels { MapViewModelFactory(requireActivity().application) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initGoogleMap()
    }

    private fun initGoogleMap() {
        val mapFragment: SupportMapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    fun initUI() {
        binding.postOfficeBtn.setOnClickListener {
            viewModel.moveToPostOffice()
        }

        binding.myLocButton.setOnClickListener {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) { viewModel.toMyLocation() }
            viewModel.toMyLocation()
        }

        binding.clearPathBtn.setOnClickListener {
            viewModel.clearPath()
        }

        // Создание тостов
        viewModel.toastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }

        // Подписка кнопки очистки карты на маршрут
        viewModel.isRouteReady.observe(viewLifecycleOwner) {
            binding.clearPathBtn.isVisible = it
        }

        // Подписка загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loadingTxt.isVisible = it
        }
    }

    fun checkPermission(permission: String, void: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                void()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), permission
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

    override fun onMapReady(p0: GoogleMap) {
        viewModel.initGoogle(p0)
    }
}