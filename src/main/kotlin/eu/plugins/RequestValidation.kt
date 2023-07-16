package eu.plugins

import LoginType
import eu.services.IValidationService
import eu.services.LoginParameters
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import org.koin.ktor.ext.inject

fun Application.configureRequestValidation() {
    val validationService by inject<IValidationService>()
    install(RequestValidation) {
        validate<LoginParameters> { params ->
            when {
                params.loginType == null -> ValidationResult.Invalid("Login type must not be null")
                params.loginType == LoginType.GOOGLE && params.idToken == null -> ValidationResult.Invalid("Id token must not be null")
                params.loginType == LoginType.EMAIL && (params.email == null || params.password == null) -> ValidationResult.Invalid(
                    "Password and email must not be null",
                )

                params.email != null && !validationService.validateEmail(params.email) -> ValidationResult.Invalid("Email is not in correct format")
                else -> ValidationResult.Valid
            }
        }
    }
}
