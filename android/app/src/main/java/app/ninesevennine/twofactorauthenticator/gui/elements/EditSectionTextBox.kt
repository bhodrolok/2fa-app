package app.ninesevennine.twofactorauthenticator.gui.elements

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer
import app.ninesevennine.twofactorauthenticator.gui.Tints

@Composable
fun EditSectionTextBox(
    title: String,
    value: String = "",
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    numberField: Boolean = false,
    error: Boolean = false
) {
    val context = LocalContext.current
    val colors = if (Theme.currentOption(context) == ThemeOption.LIGHT) Tints.light else Tints.dark
    val isUsingPitchBlack by remember { mutableStateOf(Theme.themeOption == ThemeOption.PITCH_BLACK) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, end = 4.dp),
            color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
            fontSize = 13.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterVariable
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = if (numberField) KeyboardType.Number else KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            decorationBox = { innerTextField ->
                val shape = RoundedCornerShape(12.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .then(
                            if (isUsingPitchBlack) {
                                Modifier
                                    .background(Theme.colors.background, shape)
                                    .tiltShimmer(shape = shape, color = if (error) Theme.colors.error else Theme.colors.primary)
                            } else {
                                Modifier.background(colors.medium, shape)
                            }
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (error) 6.dp else 0.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W700,
                                fontFamily = InterVariable,
                                color = if (isUsingPitchBlack) Theme.colors.primary.copy(alpha = 0.5f) else colors.light
                            )
                        }
                        innerTextField()
                    }

                    if (error) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isUsingPitchBlack) Theme.colors.error else colors.light,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            cursorBrush = SolidColor(if (isUsingPitchBlack) Theme.colors.primary else colors.dark),
            modifier = Modifier.fillMaxWidth()
        )
    }
}