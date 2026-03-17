package app.ninesevennine.twofactorauthenticator.gui.screens

import android.annotation.SuppressLint
import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Signals
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer
import app.ninesevennine.twofactorauthenticator.gui.elements.OtpTileMinimalistic
import app.ninesevennine.twofactorauthenticator.gui.elements.OtpTileNormal
import app.ninesevennine.twofactorauthenticator.utils.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import kotlin.math.max

@Serializable
object MainRoute

@Composable
fun Main(navController: NavController) {
    LaunchedEffect(Signals.intent) {
        if (Entry.rustInterface.isEditAccountSome()) {
            navController.navigate(EditRoute)
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }

    val accounts = remember(query) {
        mutableStateListOf(*Entry.rustInterface.getAccountsTruncatedFiltered(query))
    }

    val configuration = LocalConfiguration.current
    @SuppressLint("ConfigurationScreenWidthHeight")
    val columnCount = if (Config.isUsingMinimalisticTiles) {
        max(2, configuration.screenWidthDp / 264)
    } else {
        max(1, configuration.screenWidthDp / 400)
    }

    val lazyGridState = rememberLazyGridState()
    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        if (query.isEmpty()) {
            accounts.add(to.index, accounts.removeAt(from.index))
            Entry.rustInterface.reorderAccount(from.index, to.index)
        }
    }

    LaunchedEffect(query) {
        if (accounts.isNotEmpty()) {
            lazyGridState.animateScrollToItem(0)
        }
    }

    Time.Track()

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(columnCount),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp).let { if (Config.isUsingSearchOnTop) it + 52.dp + 6.dp else it },
            start = 16.dp,
            end = 16.dp,
            bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding() + 192.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(if (Config.isUsingMinimalisticTiles) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (Config.isUsingMinimalisticTiles) 8.dp else 12.dp)
    ) {
        items(count = accounts.size, key = { index -> accounts[index].id }) { index ->
            val account = accounts[index]

            ReorderableItem(reorderableLazyGridState, key = account.id) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp)
                val haptic = LocalHapticFeedback.current

                Box(
                    modifier = Modifier
                        .shadow(elevation, if (Config.isUsingMinimalisticTiles) RoundedCornerShape(20.dp) else RoundedCornerShape(26.dp))
                        .then(
                            if (query.isEmpty()) {
                                Modifier.longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                        scope.launch(Dispatchers.IO) {
                                            AccountManagement.saveAccountsUsingSeed(context)
                                        }
                                    }
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    if (Config.isUsingMinimalisticTiles) {
                        OtpTileMinimalistic(navController, account)
                    } else {
                        OtpTileNormal(navController, account)
                    }
                }
            }
        }

        if (accounts.isNotEmpty()) {
            item(span = { GridItemSpan(columnCount) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.main_showing_n_accounts, accounts.size, accounts.size),
                        fontFamily = InterVariable,
                        color = Theme.colors.primary,
                        fontWeight = FontWeight.W500,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (query.isNotBlank() && accounts.isEmpty()) {
            item(span = { GridItemSpan(columnCount) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.main_no_matching_accounts_found),
                        fontFamily = InterVariable,
                        color = Theme.colors.primary,
                        fontWeight = FontWeight.W500,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (Config.isUsingSearchOnTop) {
        TopBar(
            onSearch = {
                if (query != it) {
                    query = it
                }
            },
            onSettings = {
                navController.navigate(SettingsRoute)
            },
            onAdd = {
                navController.navigate(AddCodeRoute)
            },
            onAddLong = {
                Entry.rustInterface.setEditAccountEmpty()
                navController.navigate(EditRoute)
            }
        )
    } else {
        BottomBar(
            onSearch = {
                if (query != it) {
                    query = it
                }
            },
            onSettings = {
                navController.navigate(SettingsRoute)
            },
            onAdd = {
                navController.navigate(AddCodeRoute)
            },
            onAddLong = {
                Entry.rustInterface.setEditAccountEmpty()
                navController.navigate(EditRoute)
            }
        )
    }
}

@Composable
private fun TopBar(
    onSearch: (String) -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
    onAddLong: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val layoutDirection = LocalLayoutDirection.current

    val isUsingPitchBlack = Theme.themeOption == ThemeOption.PITCH_BLACK

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    if (Config.isUsingFocusSearch) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    val settingsInteractionSource = remember { MutableInteractionSource() }
    val isSettingsPressed by settingsInteractionSource.collectIsPressedAsState()
    val settingsScale by animateFloatAsState(if (isSettingsPressed) 0.95f else 1f)

    val addInteractionSource = remember { MutableInteractionSource() }
    val isAddPressed by addInteractionSource.collectIsPressedAsState()
    val addScale by animateFloatAsState(if (isAddPressed) 0.95f else 1f)

    val shape = RoundedCornerShape(24.dp)
    val addShape = RoundedCornerShape(30.dp)

    val keyboardPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = keyboardPadding + 6.dp > 16.dp + 48.dp

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().let { if (it > 16.dp) it + 6.dp else 16.dp }
    val bottomPadding = if (isKeyboardOpen) keyboardPadding + 4.dp else 16.dp
    val addPadding = if (isKeyboardOpen) {
        WindowInsets.displayCutout.asPaddingValues().let {
            PaddingValues(
                start = it.calculateLeftPadding(layoutDirection) + 4.dp,
                end = it.calculateRightPadding(layoutDirection) + 4.dp
            )
        }
    } else {
        PaddingValues(horizontal = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 512.dp)
                .padding(top = topPadding)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it.trim())
                },
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f)
                    .clip(shape)
                    .background(color = Theme.colors.surface, shape = shape)
                    .then(
                        if (isUsingPitchBlack) {
                            Modifier.tiltShimmer(shape)
                        } else {
                            Modifier.border(1.dp, Theme.colors.border, shape)
                        }
                    )
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700,
                    fontSize = 14.sp,
                    color = Theme.colors.primary
                ),
                cursorBrush = SolidColor(Theme.colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()

                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    }
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.mi_search),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = Theme.colors.primary
                        )

                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .weight(1f)
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.main_search),
                                    fontFamily = InterVariable,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Theme.colors.primary
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = settingsScale, scaleY = settingsScale)
                    .size(48.dp)
                    .clip(shape)
                    .background(color = Theme.colors.surface, shape = shape)
                    .then(
                        if (isUsingPitchBlack) {
                            Modifier.tiltShimmer(shape)
                        } else {
                            Modifier.border(1.dp, Theme.colors.border, shape)
                        }
                    )
                    .clickable(
                        interactionSource = settingsInteractionSource,
                        indication = ripple(color = Theme.colors.primary),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            onSettings()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mi_settings),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Theme.colors.primary
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(bottom = bottomPadding)
                .padding(addPadding),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = addScale, scaleY = addScale)
                    .size(58.dp)
                    .clip(addShape)
                    .background(color = Theme.colors.surface, shape = addShape)
                    .then(
                        if (isUsingPitchBlack) {
                            Modifier.tiltShimmer(addShape)
                        } else {
                            Modifier.border(1.dp, Theme.colors.border, addShape)
                        }
                    )
                    .combinedClickable(
                        interactionSource = addInteractionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            onAdd()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            onAddLong()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mi_add_2),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Theme.colors.primary
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    onSearch: (String) -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
    onAddLong: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val layoutDirection = LocalLayoutDirection.current

    val isUsingPitchBlack = Theme.themeOption == ThemeOption.PITCH_BLACK

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    if (Config.isUsingFocusSearch) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    val settingsInteractionSource = remember { MutableInteractionSource() }
    val isSettingsPressed by settingsInteractionSource.collectIsPressedAsState()
    val settingsScale by animateFloatAsState(if (isSettingsPressed) 0.95f else 1f)

    val addInteractionSource = remember { MutableInteractionSource() }
    val isAddPressed by addInteractionSource.collectIsPressedAsState()
    val addScale by animateFloatAsState(if (isAddPressed) 0.95f else 1f)

    val shape = RoundedCornerShape(24.dp)

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it > 16.dp) it else 16.dp }
    val keyboardPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = keyboardPadding + 6.dp > navBarPadding + 48.dp

    val bottomPadding = if (isKeyboardOpen) keyboardPadding + 4.dp else navBarPadding
    val horizontalPadding = if (isKeyboardOpen) {
        WindowInsets.displayCutout.asPaddingValues().let {
            PaddingValues(
                start = it.calculateLeftPadding(layoutDirection) + 4.dp,
                end = it.calculateRightPadding(layoutDirection) + 4.dp
            )
        }
    } else {
        PaddingValues(horizontal = navBarPadding)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = if (!isKeyboardOpen) 512.dp else 10000.dp)
                .padding(bottom = bottomPadding)
                .then(
                    if(isKeyboardOpen) {
                        Modifier.padding(horizontalPadding)
                    } else {
                        Modifier.padding(horizontal = navBarPadding)
                    }
                )
        ) {
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it.trim())
                },
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f)
                    .clip(shape)
                    .background(color = Theme.colors.surface, shape = shape)
                    .then(
                        if (isUsingPitchBlack) {
                            Modifier.tiltShimmer(shape)
                        } else {
                            Modifier.border(1.dp, Theme.colors.border, shape)
                        }
                    )
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700,
                    fontSize = 14.sp,
                    color = Theme.colors.primary
                ),
                cursorBrush = SolidColor(Theme.colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()

                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    }
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.mi_search),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = Theme.colors.primary
                        )

                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .weight(1f)
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.main_search),
                                    fontFamily = InterVariable,
                                    fontWeight = FontWeight.W500,
                                    fontSize = 14.sp,
                                    color = Theme.colors.primary
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = settingsScale, scaleY = settingsScale)
                    .size(48.dp)
                    .clip(shape)
                    .background(color = Theme.colors.surface, shape = shape)
                    .then(
                        if (isUsingPitchBlack) {
                            Modifier.tiltShimmer(shape)
                        } else {
                            Modifier.border(1.dp, Theme.colors.border, shape)
                        }
                    )
                    .clickable(
                        interactionSource = settingsInteractionSource,
                        indication = ripple(color = Theme.colors.primary),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            onSettings()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mi_settings),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Theme.colors.primary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = addScale, scaleY = addScale)
                    .size(48.dp)
                    .clip(shape)
                    .background(color = Theme.colors.surface, shape = shape)
                    .then(
                        if (isUsingPitchBlack) {
                            Modifier.tiltShimmer(shape)
                        } else {
                            Modifier.border(1.dp, Theme.colors.border, shape)
                        }
                    )
                    .combinedClickable(
                        interactionSource = addInteractionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            onAdd()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            onAddLong()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mi_add_2),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Theme.colors.primary
                )
            }
        }
    }
}