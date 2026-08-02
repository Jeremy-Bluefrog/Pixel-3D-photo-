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

    // Filtered output tilt angles
    private var filteredRoll = 0f
    private var filteredPitch = 0f

    // Sensor Fusion Complementary Filter State Variables
    private var fusedRoll = 0f
    private var fusedPitch = 0f

    // Reference orientation baseline (for auto-centering)
    private var baseRoll = 0f
    private var basePitch = 0f
    private var isBaselineSet = false

    // Absolute reference angles from Accelerometer / Gravity / Rotation Vector
    private var absRoll = 0f
    private var absPitch = 0f

    // Nanosecond timestamps for precise Gyroscope integration
    private var lastGyroTimestamp: Long = 0L
    private var hardwareEventReceived = false

    private val scope = CoroutineScope(Dispatchers.Default)
    private var simulationJob: Job? = null
    private var filterJob: Job? = null

    // Complementary filter weight for gyroscope (0.94 = 94% gyro integration + 6% gravity reference)
    private val alphaGyro = 0.94f

    // Low pass filter smoothing factor for 60fps render thread
    private val smoothingFactor = 0.35f

    fun startListening() {
        hardwareEventReceived = false
        isBaselineSet = false
        lastGyroTimestamp = 0L

        // Register hardware sensors for high-precision physical motion tracking
        gyroSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gravitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometerSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Render thread smoothing step (~60fps)
        filterJob?.cancel()
        filterJob = scope.launch {
            while (isActive) {
                delay(16)
                if (hardwareEventReceived) {
                    // Smooth transition toward fused sensor tilt
                    filteredRoll += (fusedRoll - filteredRoll) * smoothingFactor
                    filteredPitch += (fusedPitch - filteredPitch) * smoothingFactor

                    _tiltState.value = TiltData(
                        roll = filteredRoll,
                        pitch = filteredPitch,
                        isHardwareSensorActive = true
                    )

                    // Subtle auto-centering drift if user holds device still
                    if (isBaselineSet) {
                        baseRoll += (absRoll - baseRoll) * 0.002f
                        basePitch += (absPitch - basePitch) * 0.002f
                    }
                }
            }
        }

        // Simulation fallback loop when running on emulator / preview without hardware sensor
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var time = 0f
            while (isActive) {
                delay(20)
                if (!hardwareEventReceived) {
                    time += 0.04f
                    val simRoll = sin(time * 1.3f) * 0.45f
                    val simPitch = sin(time * 0.9f + 0.8f) * 0.35f

                    filteredRoll += (simRoll - filteredRoll) * 0.15f
                    filteredPitch += (simPitch - filteredPitch) * 0.15f

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

        when (sensorType) {
            Sensor.TYPE_GYROSCOPE -> {
                if (lastGyroTimestamp != 0L) {
                    val dt = (event.timestamp - lastGyroTimestamp) / 1_000_000_000f
                    if (dt in 0.001f..0.2f) {
                        // Gyro angular rates in rad/s (X = pitch, Y = roll)
                        val gyroPitchRate = event.values[0]
                        val gyroRollRate = event.values[1]

                        // Reference tilt relative to baseline
                        val refRoll = if (isBaselineSet) (absRoll - baseRoll) else 0f
                        val refPitch = if (isBaselineSet) (absPitch - basePitch) else 0f

                        // High-frequency complementary filter integration
                        fusedRoll = alphaGyro * (fusedRoll + gyroRollRate * dt * 1.2f) + (1f - alphaGyro) * refRoll
                        fusedPitch = alphaGyro * (fusedPitch + gyroPitchRate * dt * 1.2f) + (1f - alphaGyro) * refPitch

                        fusedRoll = fusedRoll.coerceIn(-1.5f, 1.5f)
                        fusedPitch = fusedPitch.coerceIn(-1.5f, 1.5f)
                    }
                }
                lastGyroTimestamp = event.timestamp
            }

            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                // Convert orientation angles to normalized tilt range (-1.0 to 1.0 representing approx 45 degrees)
                val currentPitch = (orientation[1] / (Math.PI.toFloat() / 4f)).coerceIn(-2.0f, 2.0f)
                val currentRoll = (orientation[2] / (Math.PI.toFloat() / 4f)).coerceIn(-2.0f, 2.0f)

                absRoll = currentRoll
                absPitch = currentPitch

                if (!isBaselineSet) {
                    baseRoll = currentRoll
                    basePitch = currentPitch
                    isBaselineSet = true
                    fusedRoll = 0f
                    fusedPitch = 0f
                }

                if (gyroSensor == null) {
                    fusedRoll = (absRoll - baseRoll).coerceIn(-1.5f, 1.5f)
                    fusedPitch = (absPitch - basePitch).coerceIn(-1.5f, 1.5f)
                }
            }

            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                if (rotationSensor == null) {
                    val gX = event.values[0]
                    val gY = event.values[1]
                    val gZ = event.values[2]

                    val pitchRad = atan2(gY.toDouble(), sqrt((gX * gX + gZ * gZ).toDouble())).toFloat()
                    val rollRad = atan2(-gX.toDouble(), gZ.toDouble()).toFloat()

                    val currentPitch = (pitchRad / (Math.PI.toFloat() / 4f)).coerceIn(-2.0f, 2.0f)
                    val currentRoll = (rollRad / (Math.PI.toFloat() / 4f)).coerceIn(-2.0f, 2.0f)

                    absRoll = currentRoll
                    absPitch = currentPitch

                    if (!isBaselineSet) {
                        baseRoll = currentRoll
                        basePitch = currentPitch
                        isBaselineSet = true
                        fusedRoll = 0f
                        fusedPitch = 0f
                    }

                    if (gyroSensor == null) {
                        fusedRoll = (absRoll - baseRoll).coerceIn(-1.5f, 1.5f)
                        fusedPitch = (absPitch - basePitch).coerceIn(-1.5f, 1.5f)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}



