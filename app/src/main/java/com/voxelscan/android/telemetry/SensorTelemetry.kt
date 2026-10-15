package com.voxelscan.android.telemetry

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Manages device motion sensors (Gyroscope, Accelerometer) and telemetry tracking.
 */
class SensorTelemetry(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    data class TelemetryState(
        val pitchDegrees: Float = 0f,
        val rollDegrees: Float = 0f,
        val yawDegrees: Float = 0f,
        val isLidarActive: Boolean = true,
        val isTrackingStable: Boolean = true,
        val fps: Int = 60,
        val memoryMb: Float = 38.4f,
        val hasHardwareLidar: Boolean = false
    )

    private val _state = MutableStateFlow(TelemetryState())
    val state: StateFlow<TelemetryState> = _state.asStateFlow()

    private var accelValues = FloatArray(3)
    private var gyroValues = FloatArray(3)

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelValues = event.values.clone()
                val ax = accelValues[0]
                val ay = accelValues[1]
                val az = accelValues[2]

                // Compute pitch and roll from gravity vector
                val pitch = Math.toDegrees(atan2(-ay.toDouble(), sqrt((ax * ax + az * az).toDouble()))).toFloat()
                val roll = Math.toDegrees(atan2(ax.toDouble(), az.toDouble())).toFloat()

                _state.value = _state.value.copy(
                    pitchDegrees = pitch,
                    rollDegrees = roll
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroValues = event.values.clone()
                val gz = gyroValues[2]
                val currentYaw = (_state.value.yawDegrees + Math.toDegrees(gz.toDouble() * 0.05).toFloat()) % 360f

                _state.value = _state.value.copy(
                    yawDegrees = currentYaw
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Generates a realistic surface point cloud raycast hit in front of the camera.
     * Simulates spatial plane scanning (table, walls, objects) with realistic colors.
     */
    fun sampleArDepthHit(
        timeSec: Float,
        scanRadius: Float = 1.8f
    ): PointSample {
        val angle = timeSec * 2.5f
        val rad = (sin(timeSec * 1.5) * 0.4f + 0.8f).toFloat()

        // 3D position in front of camera
        val x = cos(angle) * rad
        val y = (sin(timeSec * 3f) * 0.5f).toFloat()
        val z = (cos(timeSec * 1.2) * 0.6f + 1.2f).toFloat()

        // Jade emerald / cyber amber color theme
        val isAmberAccent = Random.nextFloat() > 0.82f
        val r = if (isAmberAccent) 245 + Random.nextInt(10) else 29 + Random.nextInt(20)
        val g = if (isAmberAccent) 150 + Random.nextInt(30) else 131 + Random.nextInt(40)
        val b = if (isAmberAccent) 56 + Random.nextInt(20) else 108 + Random.nextInt(30)

        return PointSample(x, y, z, r, g, b)
    }

    data class PointSample(
        val x: Float,
        val y: Float,
        val z: Float,
        val r: Int,
        val g: Int,
        val b: Int
    )
}
