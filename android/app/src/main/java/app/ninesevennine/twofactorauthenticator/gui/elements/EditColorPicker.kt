package app.ninesevennine.twofactorauthenticator.gui.elements

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.utils.Clipboard
import app.ninesevennine.twofactorauthenticator.utils.ColorPicker

@Composable
fun EditColorPicker(
    initialColor: Color,
    onColorChange: (Color) -> Unit = {},
    onCancel: () -> (Unit) = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val copyInteractionSource = remember { MutableInteractionSource() }
    val isCopyPressed by copyInteractionSource.collectIsPressedAsState()
    val copyScale by animateFloatAsState(if (isCopyPressed) 0.95f else 1f)

    val pasteInteractionSource = remember { MutableInteractionSource() }
    val isPastePressed by pasteInteractionSource.collectIsPressedAsState()
    val pasteScale by animateFloatAsState(if (isPastePressed) 0.95f else 1f)

    val backgroundInteractionSource = remember { MutableInteractionSource() }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it > 16.dp) it else 16.dp }

    val boxShape = RoundedCornerShape(26.dp, 26.dp, 0.dp, 0.dp)
    val pickerShape = RoundedCornerShape(12.dp)
    val shape = RoundedCornerShape(26.dp)

    val initHsv = remember(initialColor) { ColorPicker.toHsv(initialColor) }
    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var value by remember { mutableFloatStateOf(initHsv[2]) }

    val currentColor by remember { derivedStateOf { ColorPicker.fromHsv(hue, saturation, value) } }

    LaunchedEffect(currentColor) { onColorChange(currentColor) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = backgroundInteractionSource,
                indication = null,
                onClick = onCancel
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 468.dp)
                .fillMaxWidth()
                .shadow(24.dp, boxShape, clip = false)
                .background(Theme.colors.background, boxShape)
                .border(1.dp, Theme.colors.border, boxShape)
                .padding(navBarPadding),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val pureHue = remember(hue) { ColorPicker.fromHsv(hue, 1f, 1f) }

            Text(
                text = ColorPicker.toHex(currentColor),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Saturation / Value square
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(pickerShape)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        },
                ) {
                    drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pureHue)))
                    drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

                    val cx = saturation * size.width
                    val cy = (1f - value) * size.height
                    val r = 6.dp.toPx()

                    drawCircle(Color.Black.copy(alpha = 0.25f), r + 2.dp.toPx(), Offset(cx, cy))
                    drawCircle(Color.White, r, Offset(cx, cy), style = Stroke(2.dp.toPx()))
                    drawCircle(currentColor, r - 2.dp.toPx(), Offset(cx, cy))
                }

                // Vertical hue slider
                Canvas(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight()
                        .clip(pickerShape)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = ((offset.y / size.height) * 360f).coerceIn(0f, 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                hue = ((change.position.y / size.height) * 360f).coerceIn(0f, 360f)
                            }
                        },
                ) {
                    drawRect(brush = Brush.verticalGradient(ColorPicker.hueGradient))

                    val cy = (hue / 360f) * size.height
                    val hw = 6.dp.toPx()
                    val corner = CornerRadius(4.dp.toPx())

                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.25f),
                        topLeft = Offset(2.dp.toPx(), cy - hw - 2.dp.toPx()),
                        size = Size(size.width - 4.dp.toPx(), (hw + 2.dp.toPx()) * 2),
                        cornerRadius = corner,
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(4.dp.toPx(), cy - hw),
                        size = Size(size.width - 8.dp.toPx(), hw * 2),
                        cornerRadius = corner,
                        style = Stroke(2.dp.toPx()),
                    )
                    drawRoundRect(
                        color = ColorPicker.fromHsv(hue, 1f, 1f),
                        topLeft = Offset(6.dp.toPx(), cy - hw + 2.dp.toPx()),
                        size = Size(size.width - 12.dp.toPx(), (hw - 2.dp.toPx()) * 2),
                        cornerRadius = corner,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row {
                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = copyScale, scaleY = copyScale)
                        .height(52.dp)
                        .weight(1f)
                        .clip(shape)
                        .background(Theme.colors.surface)
                        .border(1.dp, Theme.colors.border, shape)
                        .clickable(
                            interactionSource = copyInteractionSource,
                            indication = ripple(color = Theme.colors.primary),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                view.playSoundEffect(SoundEffectConstants.CLICK)

                                Clipboard.copy(context, "color", ColorPicker.toHex(currentColor), false)
                            }
                        )
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.common_copy),
                        color = Theme.colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W700,
                        fontFamily = InterVariable,
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = pasteScale, scaleY = pasteScale)
                        .height(52.dp)
                        .weight(1f)
                        .clip(shape)
                        .background(Theme.colors.surface)
                        .border(1.dp, Theme.colors.border, shape)
                        .clickable(
                            interactionSource = pasteInteractionSource,
                            indication = ripple(color = Theme.colors.primary),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                view.playSoundEffect(SoundEffectConstants.CLICK)

                                val text = Clipboard.paste(context) ?: return@clickable
                                ColorPicker.fromHex(text).let { parsed ->
                                    val hsv = ColorPicker.toHsv(parsed.copy(alpha = 1f))
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                }
                            }
                        )
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.common_paste),
                        color = Theme.colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W700,
                        fontFamily = InterVariable,
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1
                    )
                }
            }
        }
    }
}