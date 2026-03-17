package app.ninesevennine.twofactorauthenticator.gui.elements

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer

@Composable
fun SectionConfidentialTextBox(
    title: String,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    error: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val isUsingPitchBlack = remember { Theme.themeOption == ThemeOption.PITCH_BLACK }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f)

    var visible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = Theme.colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Theme.colors.primary),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            visible = !visible
                        }
                    )
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = if (visible) painterResource(R.drawable.mi_visibility_off) else painterResource(R.drawable.mi_visibility),
                    contentDescription = null,
                    tint = Theme.colors.primary,
                    modifier = Modifier.padding(top = 1.dp).size(14.dp)
                )
                Text(
                    text = if (visible) "Hide" else "Show",
                    color = Theme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterVariable
                )
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                color = Theme.colors.primary
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            decorationBox = { innerTextField ->
                val shape = RoundedCornerShape(8.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(color = Theme.colors.background, shape = shape)
                        .then(
                            if (isUsingPitchBlack) {
                                Modifier.tiltShimmer(shape, color = if (error) Theme.colors.error else Color.White)
                            } else {
                                Modifier.border(1.dp, if (error) Theme.colors.error else Theme.colors.border, shape)
                            }
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W700,
                            fontFamily = InterVariable,
                            color = Theme.colors.primary.copy(alpha = 0.5f)
                        )
                    }

                    innerTextField()
                }
            },
            cursorBrush = SolidColor(Theme.colors.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}