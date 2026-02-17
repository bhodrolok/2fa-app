package app.ninesevennine.twofactorauthenticator.utils

object Pass {
    fun hasSufficientLength(value: String): Boolean {
        return value.length >= 8
    }

    fun hasNumber(value: String): Boolean {
        return value.any { it.isDigit() }
    }

    fun hasSpecialCharacter(value: String): Boolean {
        return value.any { !it.isLetterOrDigit() }
    }

    fun hasNoLeadingOrTrailingWhitespace(value: String): Boolean {
        return value == value.trim()
    }

    fun isValid(value: String): Boolean {
        return hasSufficientLength(value)
                && hasNumber(value)
                && hasSpecialCharacter(value)
                && hasNoLeadingOrTrailingWhitespace(value)
    }
}