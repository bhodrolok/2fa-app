package app.ninesevennine.twofactorauthenticator

import android.graphics.Bitmap

class RustInterface {
    companion object {
        const val LIBRARY_NAME = "appcore"
    }

    external fun features_management_aegis_import_into_hostages(json:String, passphrase: String): Int
    external fun features_management_ente_import_into_hostages(json:String, passphrase: String): Int
    external fun features_zxcvbn_f32(passphrase: String): Float

    external fun clientUnload()
    external fun setUnlockPassphraseHash(hash: ByteArray)
    external fun changeUnlockPassphraseHash(salt: ByteArray, passphrase: String)
    external fun getUnlockPassphraseHash(): ByteArray
    external fun isClientUnlockPassphraseHashEmpty(): Boolean

    external fun setEditAccountEmpty()
    external fun setEditAccountNone()
    external fun setEditAccountFromOtpAuth(url: String): Boolean
    external fun copyEditAccountIntoAccounts()
    external fun copyAccountIntoEditAccount(id: Long)
    external fun setEditAccountName(name: String)
    external fun setEditAccountIssuer(issuer: String)
    external fun setEditAccountNote(note: String)
    external fun setEditAccountSecretBase32NoPad(secretBase32NoPad: String): Boolean
    external fun setEditAccountCodeType(codeType: CodeType)
    external fun setEditAccountAlgorithm(algorithm: Algorithm)
    external fun setEditAccountCounter(counter: Long)
    external fun setEditAccountPeriod(period: Long)
    external fun setEditAccountDigits(digits: Int)
    external fun setEditAccountColor(color: Int)

    external fun isEditAccountSome(): Boolean
    external fun getEditAccountWithSecretBase32NoPad(): AccountWithSecretBase32NoPad?
    external fun getAccountsTruncatedFiltered(query: String): Array<AccountTruncated>

    external fun getAccountCode(id: Long, counter: Long, showNext: Boolean): String
    external fun reorderAccount(fromIndex: Int, toIndex: Int)
    external fun deleteAccount(id: Long)
    external fun incrementAccountCounterAndGet(id: Long): Long

    external fun exportAccountsUsingUnlockPassphraseHash(salt: ByteArray): String
    external fun exportAccounts(passphrase: String): String
    external fun exportAccountsAegis(passphrase: String): String
    external fun importAccountsUsingUnlockPassphraseHash(data: String): Int
    external fun importAccounts(passphrase: String, data: String): Int
    external fun importAccountsAegis(passphrase: String, data: String): Boolean

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

data class AccountWithSecretBase32NoPad(
    val id: Long,
    val name: String,
    val issuer: String,
    val note: String,
    val secretBase32NoPad: String,
    val codeType: CodeType,
    val algorithm: Algorithm,
    val counter: Long,
    val period: Long,
    val digits: Int,
    val color: Int
)