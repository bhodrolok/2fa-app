package app.ninesevennine.twofactorauthenticator

import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.SecureElement
import app.ninesevennine.twofactorauthenticator.features.Signals
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.features.UseIncognitoKeyboard
import app.ninesevennine.twofactorauthenticator.features.setScreenSecure
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect
import app.ninesevennine.twofactorauthenticator.gui.screens.AddCode
import app.ninesevennine.twofactorauthenticator.gui.screens.AddCodeRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.AutomaticInternalBackups
import app.ninesevennine.twofactorauthenticator.gui.screens.AutomaticInternalBackupsRoute
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
import app.ninesevennine.twofactorauthenticator.gui.screens.RecoverAccounts
import app.ninesevennine.twofactorauthenticator.gui.screens.RecoverAccountsRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectLanguage
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectLanguageRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectTheme
import app.ninesevennine.twofactorauthenticator.gui.screens.SelectThemeRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.Settings
import app.ninesevennine.twofactorauthenticator.gui.screens.SettingsRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupAutomaticInternalBackups
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupAutomaticInternalBackupsRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupBiometricUnlock
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupBiometricUnlockRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupGenerateYourRandomSeed
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupGenerateYourRandomSeedRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupGetStarted
import app.ninesevennine.twofactorauthenticator.gui.screens.SetupGetStartedRoute
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

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        ShimmerEffect.set(sensorManager)

        SecureElement.setActivity(this)

        handleIntents(intent)

        setContent {
            val navController = rememberNavController()

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
                    composable<SetupGenerateYourRandomSeedRoute> { SetupGenerateYourRandomSeed(navController) }
                    composable<SetupBiometricUnlockRoute> { SetupBiometricUnlock(navController) }
                    composable<SetupAutomaticInternalBackupsRoute> { SetupAutomaticInternalBackups(navController) }
                    composable<MainRoute> { Main(navController) }
                    composable<RecoverAccountsRoute> { RecoverAccounts(navController) }
                    composable<GreetingRoute> { Greeting(navController) }
                    composable<SettingsRoute> { Settings(navController) }
                    composable<SelectLanguageRoute> { SelectLanguage(navController) }
                    composable<SelectThemeRoute> { SelectTheme(navController) }
                    composable<BiometricUnlockRoute> { BiometricUnlock(navController) }
                    composable<AutomaticInternalBackupsRoute> { AutomaticInternalBackups(navController) }
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

        if (Theme.themeOption == ThemeOption.PITCH_BLACK) {
            ShimmerEffect.register()
        }
    }

    override fun onPause() {
        super.onPause()

        ShimmerEffect.unregister()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntents(intent)
    }

    private fun handleIntents(intent: Intent) {
        val action = intent.action
        val data = intent.data

        when (action) {
            Intent.ACTION_VIEW -> {
                data?.let { uri ->
                    if (uri.scheme == "otpauth") {
                        if (RustInterface().setEditAccountFromOtpAuth(data.toString())) {
                            Signals.intent++
                        }
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)?.let { uri ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val otpUrl = ZXing.decodeQRFromImage(uri, contentResolver)
                            if (otpUrl != null && RustInterface().setEditAccountFromOtpAuth(otpUrl)) {
                                Signals.intent++
                            }
                        }
                    }
                }
            }
        }
    }
}