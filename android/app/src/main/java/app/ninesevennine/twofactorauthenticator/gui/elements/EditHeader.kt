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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable

@Composable
fun EditHeader(
    title: String,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
    val cancelScale by animateFloatAsState(if (isCancelPressed) 0.95f else 1f)

    val doneInteractionSource = remember { MutableInteractionSource() }
    val isDonePressed by doneInteractionSource.collectIsPressedAsState()
    val doneScale by animateFloatAsState(if (isDonePressed) 0.95f else 1f)

    val shape = RoundedCornerShape(24.dp)

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Theme.colors.background,
                        Color.Transparent
                    )
                )
            )
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp + topInset,
                bottom = 8.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = cancelScale, scaleY = cancelScale)
                .size(48.dp)
                .shadow(elevation = 8.dp, shape = shape, clip = true)
                .background(Theme.colors.surface, shape)
                .border(1.dp, Theme.colors.border, shape)
                .clickable(
                    interactionSource = cancelInteractionSource,
                    indication = ripple(color = Theme.colors.primary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        view.playSoundEffect(SoundEffectConstants.CLICK)

                        onCancel()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Theme.colors.primary,
                modifier = Modifier.size(26.dp)
            )
        }

        Text(
            text = title,
            color = Theme.colors.primary,
            fontSize = 14.sp,
            fontFamily = InterVariable,
            fontWeight = FontWeight.W700
        )

        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = doneScale, scaleY = doneScale)
                .size(48.dp)
                .shadow(elevation = 8.dp, shape = shape, clip = true)
                .background(Theme.colors.surface, shape)
                .border(1.dp, Theme.colors.border, shape)
                .clickable(
                    interactionSource = doneInteractionSource,
                    indication = ripple(color = Theme.colors.primary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        view.playSoundEffect(SoundEffectConstants.CLICK)

                        onDone()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Theme.colors.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}