package app.ninesevennine.twofactorauthenticator.ui.elements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.ui.InterVariable

@Composable
fun StrengthIndicator(
    modifier: Modifier = Modifier,
    strength: Float
) {
    val s = strength.coerceIn(0f, 1f)

    val color = when {
        s <= 0.25f -> lerp(Theme.colors.veryWeak, Theme.colors.weak, s / 0.25f)
        s <= 0.50f -> lerp(Theme.colors.weak, Theme.colors.good, (s - 0.25f) / 0.25f)
        s <= 0.75f -> lerp(Theme.colors.good, Theme.colors.strong, (s - 0.50f) / 0.25f)
        else -> Theme.colors.strong
    }

    val animatedStrength by animateFloatAsState(targetValue = s)

    val label = when {
        s <= 0.25f -> stringResource(R.string.element_strength_indicator_very_weak)
        s <= 0.50f -> stringResource(R.string.element_strength_indicator_weak)
        s <= 0.75f -> stringResource(R.string.element_strength_indicator_good)
        else -> stringResource(R.string.element_strength_indicator_strong)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Theme.colors.border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedStrength)
                    .height(8.dp)
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            color = Theme.colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = InterVariable
        )
    }
}