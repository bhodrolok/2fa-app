package app.ninesevennine.twofactorauthenticator.gui.elements

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer
import app.ninesevennine.twofactorauthenticator.gui.Tints

@Composable
fun RoundedButtonTinted(
    text: String,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val context = LocalContext.current
    val colors = if (Theme.currentOption(context) == ThemeOption.LIGHT) Tints.light else Tints.dark
    val isUsingPitchBlack by remember { mutableStateOf(Theme.themeOption == ThemeOption.PITCH_BLACK) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .then(
                if (isUsingPitchBlack) {
                    Modifier.background(Theme.colors.surface).tiltShimmer(shape)
                } else {
                    Modifier.background(colors.light).border(1.dp, colors.medium, shape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    view.playSoundEffect(SoundEffectConstants.CLICK)

                    onClick()
                }
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
            fontSize = 14.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterVariable
        )
    }
}