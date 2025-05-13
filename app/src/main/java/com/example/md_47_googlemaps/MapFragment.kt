package com.example.md_47_googlemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.md_47_googlemaps.databinding.FragmentMapBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class MapFragment : Fragment(), OnMapReadyCallback {
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
        viewModel.isRouteReady.observe(viewLifecycleOwner){
            binding.clearPathBtn.isVisible = it
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        viewModel.initGoogle(p0)
    }
}