package app.ninesevennine.twofactorauthenticator

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.UseIncognitoKeyboard
import app.ninesevennine.twofactorauthenticator.features.setScreenSecure
import app.ninesevennine.twofactorauthenticator.gui.screens.AddCode
import app.ninesevennine.twofactorauthenticator.gui.screens.AddCodeRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.AutomaticBackups
import app.ninesevennine.twofactorauthenticator.gui.screens.AutomaticBackupsRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.BiometricUnlock
import app.ninesevennine.twofactorauthenticator.gui.screens.BiometricUnlockRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.Edit
import app.ninesevennine.twofactorauthenticator.gui.screens.EditRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.Export
import app.ninesevennine.twofactorauthenticator.gui.screens.ExportAegis
import app.ninesevennine.twofactorauthenticator.gui.screens.ExportAegisRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.ExportRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.Greeting
import app.ninesevennine.twofactorauthenticator.gui.screens.GreetingRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.Import
import app.ninesevennine.twofactorauthenticator.gui.screens.ImportAegis
import app.ninesevennine.twofactorauthenticator.gui.screens.ImportAegisRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.ImportRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.Main
import app.ninesevennine.twofactorauthenticator.gui.screens.MainRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectIconPacks
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectIconPacksRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectLanguage
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectLanguageRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectOtpCardStyle
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectOtpCardStyleRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectTheme
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectThemeRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.Settings
import app.ninesevennine.twofactorauthenticator.gui.screens.SettingsRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupAutomaticBackups
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupAutomaticBackupsRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupBiometricUnlock
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupBiometricUnlockRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupGetStarted
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupGetStartedRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupInitializeEncryptionKeys
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupInitializeEncryptionKeysRoute
import app.ninesevennine.twofactorauthenticator.utils.Signals
import app.ninesevennine.twofactorauthenticator.utils.ZXing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Main : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Theme.window = window
        Theme.update(this)

        setScreenSecure(this, Config.isUsingScreenSecurity)

        KeyStoreAES.setActivity(this)

        handleIntents(intent)

        setContent {
            val navController = rememberNavController()

            LaunchedEffect(Signals.authenticate) {
                if (Config.hasCompletedSetup && Entry.rustInterface.isClientSecretEmpty() && navController.currentBackStackEntry?.destination?.hasRoute<GreetingRoute>() != true) {
                    navController.navigate(GreetingRoute) {
                        popUpTo(0) { inclusive = true }
                        anim { enter = 0; exit = 0; popEnter = 0; popExit = 0 }
                    }
                }

                Signals.authenticate = 0
            }

            UseIncognitoKeyboard {
                NavHost(
                    navController = navController,
                    startDestination = if (Config.hasCompletedSetup) GreetingRoute else SetupGetStartedRoute,
                    modifier = Modifier.fillMaxSize().background(Theme.colors.background),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                ) {
                    composable<SetupGetStartedRoute> { SetupGetStarted(navController) }
                    composable<SetupInitializeEncryptionKeysRoute> { SetupInitializeEncryptionKeys(navController) }
                    composable<SetupBiometricUnlockRoute> { SetupBiometricUnlock(navController) }
                    composable<SetupAutomaticBackupsRoute> { SetupAutomaticBackups(navController) }
                    composable<MainRoute> { Main(navController) }
                    composable<GreetingRoute> { Greeting(navController) }
                    composable<SettingsRoute> { Settings(navController) }
                    composable<SelectLanguageRoute> { SelectLanguage(navController) }
                    composable<SelectThemeRoute> { SelectTheme(navController) }
                    composable<SelectOtpCardStyleRoute> { SelectOtpCardStyle(navController) }
                    composable<SelectIconPacksRoute> { SelectIconPacks(navController) }
                    composable<BiometricUnlockRoute> { BiometricUnlock(navController) }
                    composable<AutomaticBackupsRoute> { AutomaticBackups(navController) }
                    composable<ExportRoute> { Export(navController) }
                    composable<ImportRoute> { Import(navController) }
                    composable<ExportAegisRoute> { ExportAegis(navController) }
                    composable<ImportAegisRoute> { ImportAegis(navController) }
                    composable<EditRoute> { Edit(navController) }
                    composable<AddCodeRoute> { AddCode(navController) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setScreenSecure(this, Config.isUsingScreenSecurity)

        if (Entry.rustInterface.isClientSecretEmpty()) Signals.authenticate++
    }

    override fun onStop() {
        super.onStop()

        if (Config.hasCompletedSetup && !isChangingConfigurations && Config.isUsingLockOnExit) {
            if (!Signals.suppressLockOnExit) {
                Entry.rustInterface.clientUnload()
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

        val action = intent.action
        val data = intent.data

        when (action) {
            Intent.ACTION_VIEW -> {
                data?.let { uri ->
                    if (uri.scheme == "otpauth") {
                        if (Entry.rustInterface.setEditAccountFromOtpAuth(data.toString())) {
                            Signals.checkEditAccountSome = true
                        }
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)?.let { uri ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val otpUrl = ZXing.decodeQRFromImage(uri, contentResolver)
                            if (otpUrl != null && Entry.rustInterface.setEditAccountFromOtpAuth(otpUrl)) {
                                Signals.checkEditAccountSome = true
                            }
                        }
                    }
                }
            }
        }
    }
}