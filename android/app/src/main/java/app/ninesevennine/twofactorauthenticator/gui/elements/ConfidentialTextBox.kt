package app.ninesevennine.twofactorauthenticator.gui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer

@Composable
fun ConfidentialTextBox(
    title: String,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    error: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }

    var visible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )

            Row(
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { visible = !visible }
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Theme.colors.primary,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = if (visible)
                        stringResource(R.string.common_hide)
                    else
                        stringResource(R.string.common_show),
                    color = Theme.colors.primary,
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
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                color = Theme.colors.primary
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            decorationBox = { innerTextField ->
                val shape = RoundedCornerShape(12.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(color = Theme.colors.background, shape = shape)
                        .then(
                            if (Theme.themeOption == ThemeOption.PITCH_BLACK) {
                                Modifier.tiltShimmer(
                                    shape = shape,
                                    color = if (error) Theme.colors.error else Color.White
                                )
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = if (error) Theme.colors.error else Theme.colors.border,
                                    shape = shape
                                )
                            }
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    innerTextField()
                }
            },
            cursorBrush = SolidColor(Theme.colors.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}