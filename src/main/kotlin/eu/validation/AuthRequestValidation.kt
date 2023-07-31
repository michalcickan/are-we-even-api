package eu.validation

import eu.exceptions.ValidationException
import eu.models.parameters.LoginParameters
import eu.models.parameters.RegistrationParameters
import io.ktor.server.plugins.requestvalidation.*

enum class PasswordReason(val message: String) {
    SHORT("Password is too short."),
    NOT_CONTAINS_NUM("Password must contain at least one number."),
    NOT_CONTAINS_ALPHA("Password must contain at least one alphabetical character."),
}

interface IAuthRequestValidation {
    fun loginParameters(params: LoginParameters): ValidationResult
    fun registrationParameters(params: RegistrationParameters): ValidationResult
}

class AuthRequestValidation : IAuthRequestValidation {
    override fun loginParameters(params: LoginParameters): ValidationResult {
        return when {
            params.idToken == null && (params.email == null || params.password == null) ->
                ValidationResult.Invalid(ValidationException.IncorrectLoginValues.message)

            else -> ValidationResult.Valid
        }
    }

    override fun registrationParameters(params: RegistrationParameters): ValidationResult {
        if (!validateEmail(params.email)) {
            return ValidationResult.Invalid(ValidationException.InvalidEmailFormat.message)
        }
        val passwordReasons = validatePassword(params.password)
        return if (passwordReasons.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(passwordReasons.makeErrorText())
        }
    }

    private fun validateEmail(email: String): Boolean {
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        return emailRegex.matches(email)
    }

    private fun validatePassword(password: String): List<PasswordReason> {
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

private fun List<PasswordReason>.makeErrorText(): String {
    return joinToString(", ") {
        it.message
    }
}
