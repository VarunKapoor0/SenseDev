package com.example.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer

class TestActivity : AppCompatActivity() {
    
    private lateinit var viewModel: TestViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.startSensing()
        
        viewModel.sensorData.observe(this, Observer { data ->
            println("Received data: $data")
        })
    }
}
