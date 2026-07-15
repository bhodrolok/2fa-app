package app.ninesevennine.twofactorauthenticator.features.management

enum class ImportResult(val value: Int) {
    SUCCESS(0),
    INVALID_FORMAT(-1),
    UNSUPPORTED_VERSION(-2),
    INCORRECT_PASSPHRASE(-3);
}