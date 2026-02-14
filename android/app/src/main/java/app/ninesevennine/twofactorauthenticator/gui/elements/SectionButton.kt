package app.ninesevennine.twofactorauthenticator.gui.elements

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable

@Composable
fun SectionButton(
    painter: Painter? = null,
    imageVector: ImageVector? = null,
    tint: Color = Color.Unspecified,
    primaryText: String,
    secondaryText: String? = null,
    enabled: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f)

    val checkmarkScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Theme.colors.primary),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    view.playSoundEffect(SoundEffectConstants.CLICK)

                    onClick()
                },
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val iconModifier = Modifier
            .size(37.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )

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

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = primaryText,
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )

            secondaryText?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    color = Theme.colors.secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = InterVariable,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1
                )
            }
        }

        if (enabled || checkmarkScale > 0.01f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(26.dp)
                    .graphicsLayer(
                        scaleX = checkmarkScale, scaleY = checkmarkScale
                    ),
                tint = Theme.colors.primary
            )
        }
    }
}