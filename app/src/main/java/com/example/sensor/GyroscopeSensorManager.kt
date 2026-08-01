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
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt

data class TiltData(
    val roll: Float = 0f,   // X tilt (-1.0 to 1.0)
    val pitch: Float = 0f,  // Y tilt (-1.0 to 1.0)
    val isHardwareSensorActive: Boolean = false
)

class GyroscopeSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _tiltState = MutableStateFlow(TiltData())
    val tiltState: StateFlow<TiltData> = _tiltState.asStateFlow()

    // Smooth low-pass state variables
    private var filteredRoll = 0f
    private var filteredPitch = 0f
    private var targetRoll = 0f
    private var targetPitch = 0f

    // Sensor Fusion Complementary Filter Angles
    private var compRoll = 0f
    private var compPitch = 0f

    // Sensor auto-centering baseline
    private var baseRoll = 0f
    private var basePitch = 0f
    private var isBaselineSet = false

    private var lastEventTime: Long = 0
    private var hardwareEventReceived = false

    private val scope = CoroutineScope(Dispatchers.Default)
    private var simulationJob: Job? = null
    private var filterJob: Job? = null

    // Complementary Filter coefficient (0.92 gyro integration + 0.08 accelerometer/gravity correction)
    private val complementaryAlpha = 0.92f

    // EMA physics smoothing factor for fluid motion response
    private val smoothingFactor = 0.22f

    fun startListening() {
        hardwareEventReceived = false
        isBaselineSet = false

        // Register hardware sensors for high-precision physical motion tracking
        accelerometerSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gravitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
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
                    // Smooth transition toward target sensor angle
                    filteredRoll += (targetRoll - filteredRoll) * smoothingFactor
                    filteredPitch += (targetPitch - filteredPitch) * smoothingFactor

                    _tiltState.value = TiltData(
                        roll = filteredRoll,
                        pitch = filteredPitch,
                        isHardwareSensorActive = true
                    )
                }
            }
        }

        // Fallback simulation loop for emulator / desktop preview without hardware sensor
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var time = 0f
            while (isActive) {
                delay(20)
                if (!hardwareEventReceived) {
                    time += 0.04f
                    val simRoll = sin(time * 1.3f) * 0.40f
                    val simPitch = sin(time * 0.9f + 0.8f) * 0.30f

                    filteredRoll += (simRoll - filteredRoll) * 0.12f
                    filteredPitch += (simPitch - filteredPitch) * 0.12f

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
        val now = System.currentTimeMillis()

        when (sensorType) {
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
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

                targetRoll = (normRoll - baseRoll).coerceIn(-1.2f, 1.2f)
                targetPitch = (normPitch - basePitch).coerceIn(-1.2f, 1.2f)
            }

            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                val values = event.values
                val gX = values[0]
                val gY = values[1]
                val gZ = values[2]

                // Calculate tilt angles directly from Accelerometer / Gravity vector
                val pitchRad = atan2(gY.toDouble(), sqrt((gX * gX + gZ * gZ).toDouble())).toFloat()
                val rollRad = atan2(-gX.toDouble(), gZ.toDouble()).toFloat()

                val normPitch = (pitchRad / (Math.PI.toFloat() / 4f)).coerceIn(-1.5f, 1.5f)
                val normRoll = (rollRad / (Math.PI.toFloat() / 4f)).coerceIn(-1.5f, 1.5f)

                if (!isBaselineSet) {
                    baseRoll = normRoll
                    basePitch = normPitch
                    isBaselineSet = true
                }

                val accelRoll = (normRoll - baseRoll).coerceIn(-1.2f, 1.2f)
                val accelPitch = (normPitch - basePitch).coerceIn(-1.2f, 1.2f)

                // Fuse accelerometer gravity reading with gyroscope complementary filter
                if (gyroSensor == null) {
                    targetRoll = accelRoll
                    targetPitch = accelPitch
                } else {
                    compRoll = complementaryAlpha * compRoll + (1f - complementaryAlpha) * accelRoll
                    compPitch = complementaryAlpha * compPitch + (1f - complementaryAlpha) * accelPitch
                    targetRoll = compRoll
                    targetPitch = compPitch
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (lastEventTime != 0L) {
                    val dt = (now - lastEventTime) / 1000f
                    // Gyroscope angular velocity integration
                    val gyroPitchVel = event.values[0] * 1.5f
                    val gyroRollVel = event.values[1] * 1.5f

                    compPitch += gyroPitchVel * dt
                    compRoll += gyroRollVel * dt

                    targetRoll = compRoll.coerceIn(-1.2f, 1.2f)
                    targetPitch = compPitch.coerceIn(-1.2f, 1.2f)
                }
                lastEventTime = now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}


