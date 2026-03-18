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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun SectionDualButton(
    leftText: String,
    rightText: String,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val isUsingPitchBlack = remember { Theme.themeOption == ThemeOption.PITCH_BLACK }

    val shape = RoundedCornerShape(8.dp)

    val leftInteractionSource = remember { MutableInteractionSource() }
    val leftPressed by leftInteractionSource.collectIsPressedAsState()
    val leftScale by animateFloatAsState(if (leftPressed) 0.95f else 1f)

    val rightInteractionSource = remember { MutableInteractionSource() }
    val rightPressed by rightInteractionSource.collectIsPressedAsState()
    val rightScale by animateFloatAsState(if (rightPressed) 0.95f else 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(scaleX = leftScale, scaleY = leftScale)
                .height(38.dp)
                .clip(shape)
                .background(color = Theme.colors.background, shape = shape)
                .then(
                    if (isUsingPitchBlack) {
                        Modifier.tiltShimmer(shape, color = Color.White)
                    } else {
                        Modifier.border(1.dp, Theme.colors.border, shape)
                    }
                )
                .clickable(
                    interactionSource = leftInteractionSource,
                    indication = ripple(color = Theme.colors.primary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        view.playSoundEffect(SoundEffectConstants.CLICK)

                        onLeft()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leftText,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                color = Theme.colors.primary,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(scaleX = rightScale, scaleY = rightScale)
                .height(38.dp)
                .clip(shape)
                .background(color = Theme.colors.background, shape = shape)
                .then(
                    if (isUsingPitchBlack) {
                        Modifier.tiltShimmer(shape, color = Color.White)
                    } else {
                        Modifier.border(1.dp, Theme.colors.border, shape)
                    }
                )
                .clickable(
                    interactionSource = rightInteractionSource,
                    indication = ripple(color = Theme.colors.primary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        view.playSoundEffect(SoundEffectConstants.CLICK)

                        onRight()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rightText,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                color = Theme.colors.primary,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )
        }
    }
}