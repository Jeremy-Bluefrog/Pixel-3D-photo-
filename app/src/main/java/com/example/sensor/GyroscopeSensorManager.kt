package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

data class TiltData(
    val roll: Float = 0f,   // X tilt (-1.0 to 1.0)
    val pitch: Float = 0f,  // Y tilt (-1.0 to 1.0)
    val isHardwareSensorActive: Boolean = false
)

class GyroscopeSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _tiltState = MutableStateFlow(TiltData())
    val tiltState: StateFlow<TiltData> = _tiltState.asStateFlow()

    // Smooth low-pass state variables
    private var filteredRoll = 0f
    private var filteredPitch = 0f
    private var rawRoll = 0f
    private var rawPitch = 0f

    // Sensor auto-centering baseline
    private var baseRoll = 0f
    private var basePitch = 0f
    private var isBaselineSet = false

    private var lastEventTime: Long = 0
    private var hardwareEventReceived = false

    private val scope = CoroutineScope(Dispatchers.Default)
    private var simulationJob: Job? = null
    private var filterJob: Job? = null

    // Low-pass filter smoothing coefficient (0.15f = buttery smooth inertia)
    private val alpha = 0.18f

    fun startListening() {
        hardwareEventReceived = false
        isBaselineSet = false

        gyroSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Start 60 FPS motion interpolation filter loop
        filterJob?.cancel()
        filterJob = scope.launch {
            while (isActive) {
                delay(16) // ~60fps smooth physics step
                if (hardwareEventReceived) {
                    // Exponential Moving Average low-pass filter
                    filteredRoll += (rawRoll - filteredRoll) * alpha
                    filteredPitch += (rawPitch - filteredPitch) * alpha

                    _tiltState.value = TiltData(
                        roll = filteredRoll,
                        pitch = filteredPitch,
                        isHardwareSensorActive = true
                    )
                }
            }
        }

        // Fallback simulation loop for desktop preview / emulator without physical sensor
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var time = 0f
            while (isActive) {
                delay(20)
                if (!hardwareEventReceived) {
                    time += 0.04f
                    val simRoll = sin(time * 1.3f) * 0.40f
                    val simPitch = sin(time * 0.9f + 0.8f) * 0.30f
                    
                    filteredRoll += (simRoll - filteredRoll) * 0.1f
                    filteredPitch += (simPitch - filteredPitch) * 0.1f

                    _tiltState.value = TiltData(
                        roll = filteredRoll,
                        pitch = filteredPitch,
                        isHardwareSensorActive = false
                    )
                }
            }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        simulationJob?.cancel()
        filterJob?.cancel()
    }

    fun resetBaseline() {
        isBaselineSet = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        hardwareEventReceived = true

        val sensorType = event.sensor.type
        if (sensorType == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val normPitch = (orientation[1] / (Math.PI.toFloat() / 4f)).coerceIn(-1.5f, 1.5f)
            val normRoll = (orientation[2] / (Math.PI.toFloat() / 4f)).coerceIn(-1.5f, 1.5f)

            if (!isBaselineSet) {
                baseRoll = normRoll
                basePitch = normPitch
                isBaselineSet = true
            }

            rawRoll = (normRoll - baseRoll).coerceIn(-1f, 1f)
            rawPitch = (normPitch - basePitch).coerceIn(-1f, 1f)
        } else if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0] / 9.81f
            val ay = event.values[1] / 9.81f

            val normRoll = (-ax).coerceIn(-1.5f, 1.5f)
            val normPitch = ay.coerceIn(-1.5f, 1.5f)

            if (!isBaselineSet) {
                baseRoll = normRoll
                basePitch = normPitch
                isBaselineSet = true
            }

            rawRoll = (normRoll - baseRoll).coerceIn(-1f, 1f)
            rawPitch = (normPitch - basePitch).coerceIn(-1f, 1f)
        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            val now = System.currentTimeMillis()
            if (lastEventTime != 0L) {
                val dt = (now - lastEventTime) / 1000f
                rawPitch = (rawPitch + event.values[0] * dt * 1.8f).coerceIn(-1f, 1f)
                rawRoll = (rawRoll + event.values[1] * dt * 1.8f).coerceIn(-1f, 1f)
            }
            lastEventTime = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

