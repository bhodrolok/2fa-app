package app.ninesevennine.twofactorauthenticator.gui.elements

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
fun RoundedRefreshButton(
    modifier: Modifier = Modifier,
    text: String,
    spinDegrees: Float = 360f,
    spinDurationMs: Int = 600,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    val shape = RoundedCornerShape(24.dp)

    var cumulativeRotation by remember { mutableFloatStateOf(0f) }
    val iconRotation by animateFloatAsState(
        targetValue = cumulativeRotation,
        animationSpec = tween(durationMillis = spinDurationMs)
    )

    LaunchedEffect(Unit) {
        cumulativeRotation += spinDegrees
    }

    Row(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Theme.colors.primary),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    view.playSoundEffect(SoundEffectConstants.CLICK)

                    cumulativeRotation += spinDegrees

                    onClick()
                }
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.mi_refresh),
            contentDescription = null,
            modifier = Modifier.size(32.dp).graphicsLayer( rotationZ = iconRotation ),
            tint = Theme.colors.primary
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = Theme.colors.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterVariable
        )
    }
}