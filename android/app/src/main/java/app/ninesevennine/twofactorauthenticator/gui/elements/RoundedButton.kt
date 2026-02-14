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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer

@Composable
fun RoundedButton(
    text: String,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

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
            .background(Theme.colors.surface)
            .then(
                if(isUsingPitchBlack) {
                    Modifier.tiltShimmer(shape)
                } else {
                    Modifier.border(1.dp, Theme.colors.border, shape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Theme.colors.primary),
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
            color = Theme.colors.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterVariable,
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = 1
        )
    }
}