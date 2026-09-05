package app.ninesevennine.twofactorauthenticator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.UseIncognitoKeyboard
import app.ninesevennine.twofactorauthenticator.features.management.ImportResult
import app.ninesevennine.twofactorauthenticator.features.otpauth.importQRCodeIntoHostages
import app.ninesevennine.twofactorauthenticator.features.setScreenSecure
import app.ninesevennine.twofactorauthenticator.ui.screens.AddCode
import app.ninesevennine.twofactorauthenticator.ui.screens.AddCodeRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.BiometricUnlock
import app.ninesevennine.twofactorauthenticator.ui.screens.BiometricUnlockRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.ChangePassphrase
import app.ninesevennine.twofactorauthenticator.ui.screens.ChangePassphraseRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.Edit
import app.ninesevennine.twofactorauthenticator.ui.screens.EditRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.Export
import app.ninesevennine.twofactorauthenticator.ui.screens.ExportRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.ExportToAegis
import app.ninesevennine.twofactorauthenticator.ui.screens.ExportToAegisRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.Greeting
import app.ninesevennine.twofactorauthenticator.ui.screens.GreetingRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.ImportEncrypted
import app.ninesevennine.twofactorauthenticator.ui.screens.ImportEncryptedRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.ImportFromAegisEncrypted
import app.ninesevennine.twofactorauthenticator.ui.screens.ImportFromAegisEncryptedRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.Main
import app.ninesevennine.twofactorauthenticator.ui.screens.MainRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.SelectIconPacks
import app.ninesevennine.twofactorauthenticator.ui.screens.SelectIconPacksRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.SelectLanguage
import app.ninesevennine.twofactorauthenticator.ui.screens.SelectLanguageRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.SelectOtpCardStyle
import app.ninesevennine.twofactorauthenticator.ui.screens.SelectOtpCardStyleRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.SelectTheme
import app.ninesevennine.twofactorauthenticator.ui.screens.SelectThemeRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.SetPassphrase
import app.ninesevennine.twofactorauthenticator.ui.screens.SetPassphraseRoute
import app.ninesevennine.twofactorauthenticator.ui.screens.Settings
import app.ninesevennine.twofactorauthenticator.ui.screens.SettingsRoute
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Main : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!rustInterface.client_loaded_get()) {
            runBlocking(Dispatchers.IO) {
                rustInterface.client_application_directory_set(dataDir.absolutePath)

                if (rustInterface.management_accounts_load() < ImportResult.SUCCESS.value) {
                    Signals.requiresAuthentication = true
                }

                Config.load(this@Main)

                rustInterface.client_loaded_set(true)
            }
        }

        Theme.window = window
        Theme.update(this)

        setScreenSecure(activity = this, Config.isUsingScreenSecurity)

        KeyStoreAES.setActivity(this)

        if (savedInstanceState == null) {
            handleIntents(intent)
        }

        setContent {
            val navController = rememberNavController()

            LaunchedEffect(Signals.authenticate) {
                if (Signals.requiresAuthentication && !rustInterface.client_has_salt_and_key() && navController.currentBackStackEntry?.destination?.hasRoute<GreetingRoute>() != true) {
                    navController.navigate(GreetingRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                Signals.authenticate = 0
            }

            UseIncognitoKeyboard {
                NavHost(
                    navController = navController,
                    startDestination = if (Signals.requiresAuthentication) GreetingRoute else MainRoute,
                    modifier = Modifier.fillMaxSize().background(Theme.colors.background),
                    enterTransition = {
                        if (targetState.destination.hasRoute<GreetingRoute>()) EnterTransition.None
                        else slideInHorizontally(initialOffsetX = { it })
                    },
                    exitTransition = {
                        if (targetState.destination.hasRoute<GreetingRoute>()) ExitTransition.None
                        else slideOutHorizontally(targetOffsetX = { -it })
                    },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                    predictivePopEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                    predictivePopExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                ) {
                    composable<GreetingRoute> { Greeting(navController) }
                    composable<MainRoute> { Main(navController) }
                    composable<SettingsRoute> { Settings(navController) }
                    composable<SelectLanguageRoute> { SelectLanguage(navController) }
                    composable<SelectThemeRoute> { SelectTheme(navController) }
                    composable<SelectOtpCardStyleRoute> { SelectOtpCardStyle(navController) }
                    composable<SelectIconPacksRoute> { SelectIconPacks(navController) }
                    composable<BiometricUnlockRoute> { BiometricUnlock(navController) }
                    composable<SetPassphraseRoute> { SetPassphrase(navController) }
                    composable<ChangePassphraseRoute> { ChangePassphrase(navController) }
                    composable<ExportRoute> { Export(navController) }
                    composable<ImportEncryptedRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ImportEncryptedRoute>()
                        ImportEncrypted(navController, route.uri)
                    }
                    composable<ExportToAegisRoute> { ExportToAegis(navController) }
                    composable<ImportFromAegisEncryptedRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ImportFromAegisEncryptedRoute>()
                        ImportFromAegisEncrypted(navController, route.uri)
                    }
                    composable<EditRoute> { Edit(navController) }
                    composable<AddCodeRoute> { AddCode(navController) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setScreenSecure(this, Config.isUsingScreenSecurity)

        if (Signals.requiresAuthentication && !rustInterface.client_has_salt_and_key()) {
            Signals.authenticate++
        }
    }

    override fun onStop() {
        super.onStop()

        if (rustInterface.client_has_salt_and_key()) {
            if (!isChangingConfigurations && Config.isUsingLockOnExit && !Signals.suppressLockOnExit) {
                rustInterface.client_zeroize_sensitive()
                Signals.requiresAuthentication = true
                Signals.authenticate++
            }
        }

        Signals.suppressLockOnExit = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntents(intent)
    }

    private fun handleIntents(intent: Intent) {
        if (Config.isUsingBlockIntents) return

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    if (uri.scheme == "otpauth") {
                        if(rustInterface.features_otpauth_import_uri_into_hostages(uri.toString())) {
                            Signals.checkHostages = true
                            Signals.suppressFocusSearchOnLaunch = true
                        }
                    }
                }

                intent.data = null
            }
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { uri ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val bitmap = contentResolver.openInputStream(uri)?.use {
                                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.ARGB_8888
                                })
                            } ?: return@launch

                            if (importQRCodeIntoHostages(bitmap)) {
                                Signals.checkHostages = true
                                Signals.suppressFocusSearchOnLaunch = true
                            }

                            bitmap.eraseColor(0)
                            bitmap.recycle()
                        }
                    }

                    intent.removeExtra(Intent.EXTRA_STREAM)
                }
            }
        }
    }
}