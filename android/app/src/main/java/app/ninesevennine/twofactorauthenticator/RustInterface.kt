package app.ninesevennine.twofactorauthenticator

import android.graphics.Bitmap

class RustInterface {
    companion object {
        const val LIBRARY_NAME = "appcore"
    }

    external fun client_application_directory_set(applicationDirectory: String)
    external fun client_zeroize_hostages()

    external fun features_edit_get(): Account
    external fun features_edit_color_get(): Int
    external fun features_edit_color_get_as_hex(): String
    external fun features_edit_color_set(argb: Int)
    external fun features_edit_color_hue_set(hue: Float)
    external fun features_edit_color_saturation_set(saturation: Float)
    external fun features_edit_color_set(argb: String): Boolean
    external fun features_edit_name_set(name: String)
    external fun features_edit_issuer_set(issuer: String)
    external fun features_edit_note_set(note: String)
    external fun features_edit_secret_set(secretBase32NoPad: String): Boolean
    external fun features_edit_type_set(codeType: Int)
    external fun features_edit_algorithm_set(algorithm: Int)
    external fun features_edit_counter_set(counter: String): Boolean
    external fun features_edit_period_set(period: String): Boolean
    external fun features_edit_digits_set(digits: String): Boolean
    external fun features_edit_delete(id: Long)
    external fun features_management_2fas_export_hostages(passphrase: String): String
    external fun features_management_2fas_import_into_hostages(json: String, passphrase: String): Int
    external fun features_management_aegis_export_hostages(passphrase: String): String
    external fun features_management_aegis_import_into_hostages(json: String, passphrase: String): Int
    external fun features_management_internal_export_hostages(): String
    external fun features_management_plain_export_hostages(): String
    external fun features_management_plain_import_into_hostages(txt: String): Int
    external fun features_management_proton_export_hostages(passphrase: String): String

    external fun features_management_proton_import_into_hostages(json: String, passphrase: String): Int
    external fun features_zxcvbn_normalized(passphrase: String): Float

    external fun management_edit_none()
    external fun management_move_edit_into_accounts()
    external fun management_save_test()
    external fun management_copy_accounts_into_hostages()
    external fun management_merge_hostages_into_accounts()

    external fun clientUnload()
    external fun setUnlockPassphraseHash(hash: ByteArray)
    external fun changeUnlockPassphraseHash(salt: ByteArray, passphrase: String)
    external fun getUnlockPassphraseHash(): ByteArray
    external fun isClientUnlockPassphraseHashEmpty(): Boolean

    external fun setEditAccountEmpty()
    external fun setEditAccountFromOtpAuth(url: String): Boolean
    external fun copyAccountIntoEditAccount(id: Long)

    external fun isEditAccountSome(): Boolean
    external fun getEditAccountWithSecretBase32NoPad(): Account?
    external fun getAccountsTruncatedFiltered(query: String): Array<AccountTruncated>

    external fun getAccountCode(id: Long, counter: Long, showNext: Boolean): String
    external fun reorderAccount(fromIndex: Int, toIndex: Int)
    external fun incrementAccountCounterAndGet(id: Long): Long

    external fun exportAccountsUsingUnlockPassphraseHash(salt: ByteArray): String
    external fun exportAccounts(passphrase: String): String
    external fun importAccountsUsingUnlockPassphraseHash(data: String): Int
    external fun importAccounts(passphrase: String, data: String): Int

    external fun svgToBitmap(data: ByteArray, width: Int, height: Int): Bitmap?

    external fun estimatePassphraseStrength(passphrase: String): Float
}

enum class CodeType(val displayName: String) {
    HOTP("HOTP"),
    TOTP("TOTP"),
    STEAM("STEAM");
}


enum class Algorithm(val displayName: String) {
    SHA1("SHA-1"),
    SHA256("SHA-256"),
    SHA512("SHA-512");
}

data class AccountTruncated(
    val id: Long,
    val name: String,
    val issuer: String,
    val codeType: CodeType,
    val counter: Long,
    val period: Long,
    val digits: Int,
    val color: Int
)

data class Account(
    val id: Long,
    val name: String,
    val issuer: String,
    val note: String,
    val secret: String,
    val codeType: Int,
    val algorithm: Int,
    val counter: Long,
    val period: Long,
    val digits: Int,
    val color: Int
)