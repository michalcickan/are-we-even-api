package eu.validation

import eu.exceptions.ValidationException
import eu.helpers.getInvalidMessage
import eu.models.parameters.LoginParameters
import eu.models.parameters.RegistrationParameters
import io.ktor.server.plugins.requestvalidation.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class AuthValidationTest {
    private lateinit var authRequestValidation: AuthRequestValidation

    @Before
    fun setup() {
        authRequestValidation = AuthRequestValidation()
    }

    @Test
    fun `validating LoginParameter should return Invalid if email and password are null`() {
        val validationResult = authRequestValidation.loginParameters(
            LoginParameters(
                null,
                null,
                null,
            ),
        )
        assertEquals(
            ValidationException.IncorrectLoginValues.message,
            validationResult.getInvalidMessage(),
        )
    }

    @Test
    fun `validating LoginParameter should return Invalid if email is null and password not`() {
        val validationResult = authRequestValidation.loginParameters(
            LoginParameters(
                null,
                "Qwerty123!",
                null,
            ),
        )
        assertEquals(
            ValidationException.IncorrectLoginValues.message,
            validationResult.getInvalidMessage(),
        )
    }

    @Test
    fun `validating LoginParameter should return Invalid if password is null and email not`() {
        val validationResult = authRequestValidation.loginParameters(
            LoginParameters(
                null,
                null,
                "michal@test.com",
            ),
        )
        assertEquals(
            ValidationException.IncorrectLoginValues.message,
            validationResult.getInvalidMessage(),
        )
    }

    // Registration
    @Test
    fun `validating RegisterParameters should return true for valid emails`() {
        val validEmails = listOf("user@example.com", "test.user@domain.com", "name123@test-domain.com")
        validEmails.forEach { email ->
            val validationResult = authRequestValidation.registrationParameters(
                email.makeRegistrationParametersWithEmail(),
            )
            assertEquals(
                ValidationResult.Valid,
                validationResult,
            )
        }
    }

    @Test
    fun `validating RegisterParameters should return false for invalid emails`() {
        val invalidEmails = listOf("user", "user@.com", "user@domain", "user@domain.")
        invalidEmails.forEach { email ->
            val result = authRequestValidation.registrationParameters(email.makeRegistrationParametersWithEmail())
            assertEquals(
                ValidationException.InvalidEmailFormat.message,
                result.getInvalidMessage(),
            )
        }
    }

    @Test
    fun `validating RegisterParameters should return empty list for valid passwords`() {
        val validPasswords = listOf("Abcd1234", "Passw0rd", "Secret!234")
        validPasswords.forEach { password ->
            val result = authRequestValidation.loginParameters(password.makeLoginParametersForPassword())
            assertEquals(
                ValidationResult.Valid,
                result,
            )
        }
    }

    @Test
    fun `validating RegisterParameters should return appropriate reasons for invalid passwords`() {
        val invalidPasswords = listOf("short", "password", "12345678", "!@#$%^&*")
        val expectedReasons = listOf(
            arrayOf(PasswordReason.SHORT, PasswordReason.NOT_CONTAINS_NUM),
            arrayOf(PasswordReason.NOT_CONTAINS_NUM),
            arrayOf(PasswordReason.NOT_CONTAINS_ALPHA),
            arrayOf(PasswordReason.NOT_CONTAINS_ALPHA, PasswordReason.NOT_CONTAINS_NUM),
        )

        invalidPasswords.forEachIndexed { index, password ->
            val validationMessage =
                authRequestValidation
                    .registrationParameters(password.makeRegistrationParametersForPassword())
                    .getInvalidMessage() ?: fail("Should not be success $password")
            expectedReasons[index].forEach { expected ->
                assertTrue(validationMessage.contains(expected.message))
            }
        }
    }
}

private fun String.makeRegistrationParametersWithEmail(): RegistrationParameters {
    return RegistrationParameters(
        "Qwerty123!",
        this,
    )
}

private fun String.makeLoginParametersForPassword(): LoginParameters {
    return LoginParameters(
        null,
        this,
        "test@test.com",
    )
}

private fun String.makeRegistrationParametersForPassword(): RegistrationParameters {
    return RegistrationParameters(
        this,
        "test@test.com",
    )
}
