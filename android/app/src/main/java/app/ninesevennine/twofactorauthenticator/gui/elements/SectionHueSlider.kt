package app.ninesevennine.twofactorauthenticator.gui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable

@Composable
fun SectionHueSlider(
    color: Color,
    onHueChanged: (Float) -> Unit
) {
    val density = LocalDensity.current

    val hsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }

    val hue = hsv[0]
    val saturation = hsv[1]

    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    val thumbWidthDp = 6.dp
    val thumbWidthPx = with(density) { thumbWidthDp.toPx() }

    val hueColors = remember(saturation) {
        List(7) { i -> Color.hsv(i * 60f, saturation, 1f) }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hue",
                modifier = Modifier.weight(1f),
                color = Theme.colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )
        }

        val shape = RoundedCornerShape(8.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(shape)
                .background(Brush.linearGradient(hueColors))
                .border(1.dp, Theme.colors.border, shape)

                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newHue = (offset.x / trackWidthPx).coerceIn(0f, 1f) * 360f
                        onHueChanged(newHue)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val newHue = (change.position.x / trackWidthPx).coerceIn(0f, 1f) * 360f
                        onHueChanged(newHue)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        val usable = trackWidthPx - thumbWidthPx
                        val x = (hue / 360f) * usable
                        IntOffset(x.toInt(), 0)
                    }
                    .width(thumbWidthDp)
                    .fillMaxHeight()
                    .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    .padding(1.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
        }
    }
}