package com.example.md_47_googlemaps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.md_47_googlemaps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    fun initUI() {
        binding.postOfficeBtn.setOnClickListener {

        }

        binding.myLocButton.setOnClickListener {

        }

        binding.clearPathBtn.setOnClickListener {

        }
        binding.clearPathBtn.isVisible = false
    }
}