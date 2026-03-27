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
 * Returns the closest human-readable colour name for a hex string.
 * Used to pre-populate the colour name field; the user can always override it.
 */
fun labelForPickedColor(hex: String): String {
    val argb = runCatching { AndroidColor.parseColor(hex) }.getOrElse { return "Custom" }
    val r = AndroidColor.red(argb)
    val g = AndroidColor.green(argb)
    val b = AndroidColor.blue(argb)

    data class NC(val name: String, val r: Int, val g: Int, val b: Int)

    val palette = listOf(
        NC("Black", 0, 0, 0), NC("Dark Gray", 64, 64, 64), NC("Gray", 128, 128, 128),
        NC("Silver", 192, 192, 192), NC("Light Gray", 211, 211, 211), NC("White", 255, 255, 255),
        NC("Ivory", 255, 255, 240), NC("Cream", 255, 253, 208), NC("Beige", 245, 245, 220),
        NC("Champagne", 247, 231, 206), NC("Tan", 210, 180, 140), NC("Camel", 193, 154, 107),
        NC("Gold", 212, 175, 55), NC("Yellow", 255, 215, 0), NC("Olive", 107, 142, 35),
        NC("Brown", 139, 69, 19), NC("Rust", 183, 65, 14), NC("Orange", 255, 140, 0),
        NC("Coral", 255, 127, 80), NC("Red", 220, 20, 60), NC("Burgundy", 128, 0, 32),
        NC("Blush", 255, 182, 193), NC("Pink", 255, 105, 180), NC("Hot Pink", 255, 20, 147),
        NC("Rose", 255, 0, 127), NC("Mauve", 153, 102, 153), NC("Lavender", 230, 230, 250),
        NC("Violet", 138, 43, 226), NC("Purple", 128, 0, 128), NC("Plum", 142, 69, 133),
        NC("Indigo", 75, 0, 130), NC("Navy", 0, 0, 128), NC("Blue", 30, 100, 200),
        NC("Sky Blue", 135, 206, 235), NC("Cyan", 0, 188, 212), NC("Teal", 0, 128, 128),
        NC("Mint", 152, 255, 152), NC("Sage", 143, 188, 143), NC("Green", 34, 139, 34),
        NC("Slate", 112, 128, 144),
    )

    return palette.minByOrNull { nc ->
        val dr = (nc.r - r).toLong()
        val dg = (nc.g - g).toLong()
        val db = (nc.b - b).toLong()
        dr * dr + dg * dg + db * db
    }?.name ?: "Custom"
}

/** Full list of colour names shown as dropdown suggestions in the Add Item form. */
internal val COLOR_NAME_SUGGESTIONS = listOf(
    "Black", "Dark Gray", "Gray", "Silver", "Light Gray", "White",
    "Ivory", "Cream", "Beige", "Champagne", "Tan", "Camel",
    "Gold", "Yellow", "Olive", "Brown", "Rust", "Orange",
    "Coral", "Red", "Burgundy", "Blush", "Pink", "Hot Pink",
    "Rose", "Mauve", "Lavender", "Violet", "Purple", "Plum",
    "Indigo", "Navy", "Blue", "Sky Blue", "Cyan", "Teal",
    "Mint", "Sage", "Green", "Slate",
)

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
