package app.ninesevennine.twofactorauthenticator.gui.screens

import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.StrengthIndicator
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.AccountManagement
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.security.SecureRandom

@Serializable
object AutomaticBackupsRoute

private const val TAG = "AutomaticBackups"

@Composable
fun AutomaticBackups(navController: NavController) {
    val context = LocalContext.current

    var passphrase by remember { mutableStateOf("") }
    var passphraseStrength by remember { mutableFloatStateOf(0f) }
    var confirmPassphrase by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf(false) }
    var recoveryErrorCode by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    val promptTitle = stringResource(R.string.common_title_confirm_biometrics)
    val promptNegative = stringResource(R.string.common_button_cancel)

    val isPassphraseValid = passphraseStrength > 0.5f

    // copy otherwise ui change will be visible on pop back.
    var isUsingAutomaticBackups by remember { mutableStateOf(Config.isUsingAutomaticBackups) }

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topInset + 80.dp))

                Icon(
                    painter = painterResource(R.drawable.mi_settings_backup_restore),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Theme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.common_title_automatic_backups),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 18.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.common_description_automatic_backups_part_1),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.common_description_automatic_backups_part_2),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal
                )

                if (AccountManagement.hasAccountsUsingAutomaticBackupsKey(context) && isUsingAutomaticBackups) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.common_description_automatic_backups_found),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.info,
                        fontSize = 14.sp,
                        fontFamily = InterVariable
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ConfidentialTextBox(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = stringResource(R.string.common_title_passphrase),
                        value = passphrase,
                        onValueChange = {
                            if (!isLoading) {
                                passphrase = it
                            }

                            recoveryError = false
                        },
                        error = passphrase.isEmpty() || recoveryError
                    )

                    AnimatedVisibility(recoveryError) {
                        Text(
                            text = when (recoveryErrorCode) {
                                AccountManagement.IMPORT_ERROR_NO_ACCOUNTS -> stringResource(R.string.common_import_error_no_accounts)
                                AccountManagement.IMPORT_ERROR_INCORRECT_PASSPHRASE -> stringResource(R.string.common_import_error_incorrect_passphrase)
                                AccountManagement.IMPORT_ERROR_INCORRECT_VERSION -> stringResource(R.string.common_import_error_incorrect_version)
                                AccountManagement.IMPORT_ERROR_INVALID_FILE -> stringResource(R.string.common_import_error_invalid)
                                else -> "Unknown error"
                            },
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp),
                            color = Theme.colors.veryWeak,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = InterVariable
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RoundedButton(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        text = if (isLoading) spinnerText() else stringResource(R.string.common_button_restore)
                    ) {
                        if (isLoading) return@RoundedButton

                        isLoading = true
                        recoveryError = false

                        scope.launch(Dispatchers.IO) {
                            val data = AccountManagement.getAccountsUsingAutomaticBackupsKey(context)
                            if (data == null) {
                                isLoading = false
                                return@launch
                            }

                            var result = Entry.rustInterface.importAccountsUsingAutomaticBackupsKey003(passphrase, data)
                            if (result == AccountManagement.IMPORT_ERROR_INCORRECT_VERSION) {
                                result = Entry.rustInterface.importAccounts(passphrase, data)
                            }

                            if (result > 0) {
                                Logger.i(TAG, "successfully restored $result account(s)")

                                AccountManagement.saveAccountsUsingKey(context)

                                Signals.accountAdded = true

                                withContext(Dispatchers.Main) {
                                    navController.popBackStack(MainRoute, false)
                                }
                            } else {
                                recoveryError = true
                                recoveryErrorCode = result
                                isLoading = false
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))

                    ConfidentialTextBox(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = stringResource(R.string.common_title_passphrase),
                        value = passphrase,
                        onValueChange = {
                            if (!isLoading) {
                                passphrase = it
                                passphraseStrength = Entry.rustInterface.estimatePassphraseStrength(it)
                            }
                        }
                    )

                    StrengthIndicator(
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                        strength = passphraseStrength
                    )

                    AnimatedVisibility(isPassphraseValid) {
                        ConfidentialTextBox(
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                            title = stringResource(R.string.common_title_confirm_passphrase),
                            value = confirmPassphrase,
                            onValueChange = { if (!isLoading) confirmPassphrase = it },
                            error = passphrase != confirmPassphrase
                        )
                    }

                    AnimatedVisibility(isPassphraseValid && passphrase == confirmPassphrase) {
                        RoundedButton(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            text = if (isLoading) spinnerText() else stringResource(R.string.common_button_enable)
                        ) {
                            if (isLoading) return@RoundedButton

                            isLoading = true

                            scope.launch(Dispatchers.IO) {
                                val oldSecret = if (Config.hasMigratedToSecret003) {
                                    Entry.rustInterface.getSecret003()
                                } else if (Config.hasMigratedToSecret002) {
                                    Entry.rustInterface.getSecret002()
                                } else {
                                    Entry.rustInterface.getSecret001()
                                }

                                if (Config.hasMigratedToSecret003) {
                                    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                                    Entry.rustInterface.setAutomaticBackupsPassphrase003(salt, passphrase)
                                    Config.automaticBackupsSalt = Base64.encodeToString(salt, Base64.DEFAULT)
                                } else if (Config.hasMigratedToSecret002) {
                                    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                                    Entry.rustInterface.setAutomaticBackupsPassphrase002(salt, passphrase)
                                    Config.automaticBackupsSalt = Base64.encodeToString(salt, Base64.NO_WRAP)
                                } else {
                                    Entry.rustInterface.setAutomaticBackupsPassphrase001(passphrase)
                                }

                                withContext(Dispatchers.Main) {
                                    KeyStoreAES.saveSecret(
                                        title = promptTitle,
                                        negative = promptNegative,
                                        secret = if (Config.hasMigratedToSecret003) {
                                            Entry.rustInterface.getSecret003()
                                        } else if (Config.hasMigratedToSecret002) {
                                            Entry.rustInterface.getSecret002()
                                        } else {
                                            Entry.rustInterface.getSecret001()
                                        },
                                        onSuccess = {
                                            AccountManagement.saveAccountsUsingAutomaticBackupsKey(context)

                                            Config.isUsingAutomaticBackups = true

                                            Logger.i(TAG, "automatic backups set to true")

                                            Config.save(context)

                                            navController.popBackStack()
                                        },
                                        onError = {
                                            if (Config.hasMigratedToSecret003) {
                                                Entry.rustInterface.setSecret003(oldSecret)
                                            } else if (Config.hasMigratedToSecret002) {
                                                Entry.rustInterface.setSecret002(oldSecret)
                                            } else {
                                                Entry.rustInterface.setSecret001(oldSecret)
                                            }
                                            isLoading = false
                                        },
                                        onCancel = {
                                            if (Config.hasMigratedToSecret003) {
                                                Entry.rustInterface.setSecret003(oldSecret)
                                            } else if (Config.hasMigratedToSecret002) {
                                                Entry.rustInterface.setSecret002(oldSecret)
                                            } else {
                                                Entry.rustInterface.setSecret001(oldSecret)
                                            }
                                            Logger.i(TAG, "biometric prompt canceled")
                                            isLoading = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }

        if (isUsingAutomaticBackups) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoundedButtonClean(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = bottomInset),
                        text = stringResource(R.string.common_button_disable)
                    ) {
                        Config.isUsingAutomaticBackups = false

                        Logger.i(TAG, "automatic backups set to false")

                        Config.save(context)
                        navController.popBackStack()
                    }
                }
            }
        }

        StartButtonHeader("") {
            if (isLoading) return@StartButtonHeader

            navController.popBackStack()
        }
    }
}