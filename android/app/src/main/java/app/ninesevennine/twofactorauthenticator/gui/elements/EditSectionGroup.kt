package app.ninesevennine.twofactorauthenticator.gui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer
import app.ninesevennine.twofactorauthenticator.gui.Tints

@Composable
fun EditSectionGroup(
    title: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = if (Theme.currentOption(context) == ThemeOption.LIGHT) Tints.light else Tints.dark
    val isUsingPitchBlack by remember { mutableStateOf(Theme.themeOption == ThemeOption.PITCH_BLACK) }

    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 12.dp),
            color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
            fontSize = 14.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterVariable
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (isUsingPitchBlack) {
                        Modifier.background(Theme.colors.surface).tiltShimmer(shape)
                    } else {
                        Modifier.background(colors.light).border(1.dp, colors.medium, shape)
                    }
                )
        ) {
            content()
        }
    }
}