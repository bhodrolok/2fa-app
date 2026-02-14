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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
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
fun <T : Enum<T>> EditSectionDropdownSingleChoice(
    title: String,
    options: Array<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    label: (T) -> String,
    color: Int
) {
    val context = LocalContext.current
    val colors = if (Theme.currentOption(context) == ThemeOption.LIGHT) Tints.light else Tints.dark
    val isUsingPitchBlack by remember { mutableStateOf(Theme.themeOption == ThemeOption.PITCH_BLACK) }

    val shape = RoundedCornerShape(12.dp)
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, end = 4.dp),
            color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
            fontSize = 13.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterVariable
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    color = if (isUsingPitchBlack) Theme.colors.background else colors.medium,
                    shape = shape
                )
                .then(
                    if (isUsingPitchBlack) Modifier.tiltShimmer(shape = shape, color = Theme.colors.primary)
                    else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded }
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label(selectedOption),
                    color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterVariable
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isUsingPitchBlack) Theme.colors.primary else colors.light
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .then(
                        if (isUsingPitchBlack) {
                            Modifier.background(Theme.colors.background)
                        } else {
                            Modifier
                                .graphicsLayer(colorFilter = ColorFilter.tint(Color(color), BlendMode.Multiply))
                                .background(colors.light)
                        }
                    )
            ) {
                Column {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label(option),
                                    fontFamily = InterVariable,
                                    fontWeight = FontWeight.W700,
                                    fontSize = 13.sp,
                                    color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark
                                )
                            },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}