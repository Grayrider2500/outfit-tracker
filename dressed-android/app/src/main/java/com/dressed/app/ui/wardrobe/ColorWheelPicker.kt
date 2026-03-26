package com.dressed.app.ui.wardrobe

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dressed.app.data.model.WardrobeColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

fun hsvToHex(h: Float, s: Float, v: Float): String {
    val argb = AndroidColor.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))
    return String.format("#%06X", 0xFFFFFF and argb)
}

fun hsvToComposeColor(h: Float, s: Float, v: Float): Color {
    val argb = AndroidColor.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))
    return Color(argb)
}

/**
 * Picks a friendly label: nearest wardrobe palette name if close in RGB, else "Custom shade".
 */
fun labelForPickedColor(hex: String): String {
    val picked = runCatching {
        AndroidColor.parseColor(hex)
    }.getOrElse { return "Custom shade" }

    var bestName = "Custom shade"
    var bestDist = 120f
    for (swatch in WardrobeColors.PALETTE) {
        val c = runCatching { AndroidColor.parseColor(swatch.hex) }.getOrNull() ?: continue
        val d = rgbDistance(picked, c)
        if (d < bestDist) {
            bestDist = d
            bestName = swatch.name
        }
    }
    return if (bestDist < 45f) bestName else "Custom shade"
}

private fun rgbDistance(a: Int, b: Int): Float {
    val ar = (a shr 16) and 0xFF
    val ag = (a shr 8) and 0xFF
    val ab = a and 0xFF
    val br = (b shr 16) and 0xFF
    val bg = (b shr 8) and 0xFF
    val bb = b and 0xFF
    val dr = (ar - br).toFloat()
    val dg = (ag - bg).toFloat()
    val db = (ab - bb).toFloat()
    return sqrt(dr * dr + dg * dg + db * db)
}

@Composable
fun ColorWheelPicker(
    hue: Float,
    onHueChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Drag the ring to choose hue. Use the sliders for saturation and brightness.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        val preview = hsvToComposeColor(hue, saturation, brightness)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                hsvToHex(hue, saturation, brightness).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .size(48.dp, 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(preview)
            )
        }
        Spacer(Modifier.height(12.dp))

        HueRing(
            hue = hue,
            onHueChange = onHueChange,
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(8.dp))
        Text("Saturation", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = saturation,
            onValueChange = onSaturationChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = preview,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Text("Brightness", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = preview,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun HueRing(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.background
    val sweep = Brush.sweepGradient(
        0f to Color(0xFFFF0000),
        60f / 360f to Color(0xFFFFFF00),
        120f / 360f to Color(0xFF00FF00),
        180f / 360f to Color(0xFF00FFFF),
        240f / 360f to Color(0xFF0000FF),
        300f / 360f to Color(0xFFFF00FF),
        1f to Color(0xFFFF0000),
    )

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outer = min(size.width, size.height) / 2f * 0.92f
                val inner = outer * 0.58f

                fun updateFrom(offset: Offset) {
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist in inner..outer) {
                        var angle = (atan2(dy, dx) * 180f / PI).toFloat()
                        if (angle < 0f) angle += 360f
                        onHueChange(angle)
                    }
                }

                updateFrom(down.position)
                drag(down.id) { change ->
                    updateFrom(change.position)
                    change.consume()
                }
            }
        },
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val outer = min(this.size.width, this.size.height) / 2f * 0.92f
        val inner = outer * 0.58f

        drawCircle(
            brush = sweep,
            radius = outer,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = bg,
            radius = inner,
            center = Offset(cx, cy),
        )

        val rad = (hue * PI / 180f).toFloat()
        val mid = (outer + inner) / 2f
        val hx = cx + cos(rad) * mid
        val hy = cy + sin(rad) * mid
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(hx, hy),
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = Color(0xFF333333),
            radius = 6.dp.toPx(),
            center = Offset(hx, hy),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
