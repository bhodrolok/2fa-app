package app.ninesevennine.twofactorauthenticator.gui.elements

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable

@Composable
fun RoundedRadioButton(
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    imageVector: ImageVector? = null,
    tint: Color = Color.Unspecified,
    text: String,
    enabled: Boolean = false,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    val shape = RoundedCornerShape(24.dp)

    val checkmarkScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Box(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center
    ) {
        if (enabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Theme.colors.surface, shape)
                    .border(1.dp, Theme.colors.border, shape)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = Theme.colors.primary),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        view.playSoundEffect(SoundEffectConstants.CLICK)

                        onClick()
                    }
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = Modifier.size(32.dp)

            when {
                painter != null -> {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        modifier = iconModifier,
                        tint = tint
                    )
                }

                imageVector != null -> {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        modifier = iconModifier,
                        tint = tint
                    )
                }

                else -> {
                    Icon(
                        painter = painterResource(R.drawable.unknown001),
                        contentDescription = null,
                        modifier = iconModifier,
                        tint = tint
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable
            )

            if (enabled || checkmarkScale > 0.01f) {
                Icon(
                    painter = painterResource(R.drawable.mi_check),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(28.dp)
                        .graphicsLayer(
                            scaleX = checkmarkScale, scaleY = checkmarkScale
                        ),
                    tint = Theme.colors.primary
                )
            }
        }
    }
}