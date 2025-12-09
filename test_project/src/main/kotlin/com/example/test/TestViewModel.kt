package com.example.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestViewModel(private val repository: TestRepository) : ViewModel() {
    
    private val _sensorData = MutableLiveData<Float>()
    val sensorData: LiveData<Float> = _sensorData
    
    fun startSensing() {
        repository.startListening { data ->
            updateData(data)
        }
    }
    
    private fun updateData(data: Float) {
        _sensorData.postValue(data)
    }
}
