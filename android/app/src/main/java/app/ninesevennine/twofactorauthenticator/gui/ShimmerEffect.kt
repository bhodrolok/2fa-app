package app.ninesevennine.twofactorauthenticator.gui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object ShimmerEffect : SensorEventListener {
    var sensorManager: SensorManager? = null
    var rotationSensor: Sensor? = null

    var pitch = mutableFloatStateOf(0f)
    var roll = mutableFloatStateOf(0f)

    fun set(manager: SensorManager) {
        sensorManager = manager
        rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    fun register() {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregister() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val newPitch = orientation[1]
            val newRoll = orientation[2]

            if (newPitch.isFinite()) pitch.floatValue = newPitch
            if (newRoll.isFinite()) roll.floatValue = newRoll
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ignore
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a < -PI.toFloat()) a += (2 * PI).toFloat()
        while (a > PI.toFloat()) a -= (2 * PI).toFloat()
        return a
    }

    @Composable
    fun Modifier.tiltShimmer(
        shape: RoundedCornerShape,
        peakAlpha: Float = 0.5f,
        edgeAlpha: Float = 0.1f,
        strokeWidthDp: Float = 1f,
        delta: Float = 0.1f,
        color: Color = Color.White
    ): Modifier = composed {
        val pitchValue = pitch.floatValue
        val rollValue = -roll.floatValue

        val pitchOffset = pitchValue - Math.toRadians(-45.0).toFloat()
        val baseAngle = Math.toRadians(-75.0).toFloat()
        val rawAngle = normalizeAngle(baseAngle + rollValue * 3f + pitchOffset * 3f)
        val minAngle = Math.toRadians(60.0).toFloat()
        val maxAngle = Math.toRadians(114.0).toFloat()
        val target = (rawAngle.absoluteValue.coerceIn(minAngle, maxAngle) * 100f).roundToInt() / 100f

        val shimmerAngle by animateFloatAsState( targetValue = target, animationSpec = spring(stiffness = 30f, dampingRatio = 0.9f) )

        val centerStop = 0.5f
        val safeDelta = delta.coerceIn(0.01f, 0.48f)
        val leftEdge = centerStop - safeDelta
        val rightEdge = centerStop + safeDelta

        drawWithContent {
            drawContent()

            val angle = shimmerAngle
            if (!angle.isFinite() || size.width <= 0f || size.height <= 0f) return@drawWithContent

            val cornerPx = shape.topStart.toPx(size, this)
            val strokePx = strokeWidthDp.dp.toPx()

            val dirX = cos(angle)
            val dirY = sin(angle)
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxSide = minOf(size.width, size.height)
            val length = maxSide * 1.6f
            val start = center - Offset(dirX * length, dirY * length)
            val end = center + Offset(dirX * length, dirY * length)

            if (!start.x.isFinite() || !start.y.isFinite() || !end.x.isFinite() || !end.y.isFinite()) return@drawWithContent

            val gradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    leftEdge to color.copy(alpha = edgeAlpha),
                    centerStop to color.copy(alpha = peakAlpha),
                    rightEdge to color.copy(alpha = edgeAlpha),
                    1.0f to Color.Transparent
                ),
                start = start,
                end = end
            )

            drawRoundRect(
                brush = gradient,
                size = size,
                cornerRadius = CornerRadius(cornerPx),
                style = Stroke(width = strokePx)
            )
        }
    }
}