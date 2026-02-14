package app.ninesevennine.twofactorauthenticator.gui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer
import app.ninesevennine.twofactorauthenticator.gui.Tints

@Composable
fun EditSectionConfidentialTextBox(
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

    val interactionSource = remember { MutableInteractionSource() }

    var visible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable
            )

            Row(
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { visible = !visible }
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = if (visible)
                        stringResource(R.string.common_hide)
                    else
                        stringResource(R.string.common_show),
                    color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterVariable
                )
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = if (numberField) KeyboardType.NumberPassword else KeyboardType.Password,
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