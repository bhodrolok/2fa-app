package app.ninesevennine.twofactorauthenticator

import android.graphics.Bitmap

class RustInterface {
    external fun client_application_directory_set(applicationDirectory: String)

    external fun features_edit_get(): Account
    external fun features_edit_color_get(): Int
    external fun features_edit_color_get_as_hex(): String
    external fun features_edit_color_set(argb: Int)
    external fun features_edit_color_hue_set(hue: Float)
    external fun features_edit_color_saturation_set(saturation: Float)
    external fun features_edit_color_set(argb: String)
    external fun features_edit_name_set(name: String): Boolean
    external fun features_edit_issuer_set(issuer: String)
    external fun features_edit_note_set(note: String)
    external fun features_edit_secret_set(secretBase32NoPad: String): Boolean
    external fun features_edit_type_set(codeType: Int)
    external fun features_edit_algorithm_set(algorithm: Int)
    external fun features_edit_counter_set(counter: String): Boolean
    external fun features_edit_period_set(period: String): Boolean
    external fun features_edit_digits_set(digits: String): Boolean
    external fun features_edit_delete(id: Long)

    external fun features_main_search(query: String): Array<AccountShort>
    external fun features_main_reorder(from: Long, to: Long)

    external fun features_management_aegis_export_accounts(passphrase: String): ByteArray
    external fun features_management_aegis_import_into_hostages(bytes: ByteArray, passphrase: String): Int

    external fun features_otp_generate(id: Long, counter: Long, next: Boolean, revealed: Boolean): String
    external fun features_otp_counter_increment(id: Long): Long

    external fun features_otpauth_import_uri_into_hostages(uri: String): Boolean
    external fun features_otpauth_import_mafile_into_hostages(json: String): Boolean
    external fun features_otpauth_import_qrcode_into_hostages(width: Int, height: Int, pixels: IntArray): Boolean

    external fun features_zxcvbn_normalized(passphrase: String): Float

    external fun management_accounts_load()
    external fun management_accounts_save()

    external fun management_edit_none()
    external fun management_edit_default()

    external fun management_import_edit_into_accounts()
    external fun management_import_account_into_edit(id: Long)
    external fun management_import_hostages_into_accounts()

    external fun clientUnload()
    external fun setUnlockPassphraseHash(hash: ByteArray)
    external fun changeUnlockPassphraseHash(salt: ByteArray, passphrase: String)
    external fun getUnlockPassphraseHash(): ByteArray
    external fun isClientUnlockPassphraseHashEmpty(): Boolean


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

data class AccountShort(
    val id: Long,
    val name: String,
    val issuer: String,
    val codeType: Int,
    val counter: Long,
    val period: Long,
    val color: Int
)