package eu.services

enum class PasswordReason {
    SHORT, NOT_CONTAINS_NUM, NOT_CONTAINS_ALPHA
}

interface IValidationService {
    fun validateEmail(email: String): Boolean
    fun validatePassword(password: String): List<PasswordReason>
}

class ValidationService : IValidationService {
    override fun validateEmail(email: String): Boolean {
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        return emailRegex.matches(email)
    }

    override fun validatePassword(password: String): List<PasswordReason> {
        val errors = mutableListOf<PasswordReason>()

        if (password.length < 8) {
            errors.add(PasswordReason.SHORT)
        }

        if (!password.any { it.isDigit() }) {
            errors.add(PasswordReason.NOT_CONTAINS_NUM)
        }

        if (!password.any { it.isLetter() }) {
            errors.add(PasswordReason.NOT_CONTAINS_ALPHA)
        }
        return errors
    }
}
