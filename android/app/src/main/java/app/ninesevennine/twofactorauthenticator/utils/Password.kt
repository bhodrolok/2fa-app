package app.ninesevennine.twofactorauthenticator.utils

object Password {
    fun hasSufficientLength(password: String): Boolean {
        return password.length >= 8
    }

    fun hasNumber(password: String): Boolean {
        return password.any { it.isDigit() }
    }

    fun hasSpecialCharacter(password: String): Boolean {
        return password.any { !it.isLetterOrDigit() }
    }

    fun hasNoLeadingOrTrailingWhitespace(password: String): Boolean {
        return password == password.trim()
    }

    fun isValid(password: String): Boolean {
        return hasSufficientLength(password)
                && hasNumber(password)
                && hasSpecialCharacter(password)
                && hasNoLeadingOrTrailingWhitespace(password)
    }
}