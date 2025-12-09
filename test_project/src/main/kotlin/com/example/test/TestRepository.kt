package com.example.test

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class TestRepository(private val sensorManager: SensorManager) : SensorEventListener {
    
    private var listener: ((Float) -> Unit)? = null
    
    fun startListening(callback: (Float) -> Unit) {
        listener = callback
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        listener?.invoke(x)
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
