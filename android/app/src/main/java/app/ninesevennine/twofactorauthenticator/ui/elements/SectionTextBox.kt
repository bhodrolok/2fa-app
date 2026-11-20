package app.ninesevennine.twofactorauthenticator.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.ui.InterVariable
import kotlinx.coroutines.flow.drop

@Composable
fun SectionTextBox(
    title: String,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    numberField: Boolean = false,
    error: Boolean = false
) {
    val state = remember { TextFieldState(value) }

    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .drop(1)
            .collect {
                if (it != currentValue) {
                    currentOnValueChange(it)
                }
            }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = Theme.colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )
        }

        BasicTextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterVariable,
                color = Theme.colors.primary
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = if (numberField) KeyboardType.Number else KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            cursorBrush = SolidColor(Theme.colors.primary),
            decorator = { innerTextField ->
                val shape = RoundedCornerShape(8.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(color = Theme.colors.background, shape = shape)
                        .border(1.dp, if (error) Theme.colors.veryWeak else Theme.colors.border, shape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (state.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W700,
                            fontFamily = InterVariable,
                            color = Theme.colors.primary.copy(alpha = 0.5f)
                        )
                    }

                    innerTextField()
                }
            }
        )
    }
}