package eu.plugins

import eu.models.parameters.LoginParameters
import eu.models.parameters.RegistrationParameters
import eu.models.responses.GenericResponse
import eu.services.IValidationService
import eu.services.PasswordReason
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.koin.ktor.ext.inject

fun Application.configureRequestValidation() {
    val validationService by inject<IValidationService>()
    install(RequestValidation) {
        validate<LoginParameters> { params ->
            when {
                params.email != null && !validationService.validateEmail(params.email) -> ValidationResult.Invalid("Email is not in correct format")
                else -> ValidationResult.Valid
            }
        }

        validate<RegistrationParameters> { params ->
            if (params.email == null || params.password == null || !validationService.validateEmail(params.email)) {
                ValidationResult.Invalid("Email or password is not correct")
            } else {
                val passwordReasons = validationService.validatePassword(params.password)
                if (passwordReasons.isEmpty()) {
                    ValidationResult.Valid
                } else {
                    ValidationResult.Invalid(passwordReasons.makeErrorText())
                }
            }
        }
    }

    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                GenericResponse.createError<Unit>(cause.reasons.joinToString()),
            )
        }
    }
}

private fun List<PasswordReason>.makeErrorText(): String {
    return joinToString(", ") {
        when (it) {
            PasswordReason.NOT_CONTAINS_ALPHA -> "Password must contain at least one alphabetic character"
            PasswordReason.SHORT -> "Password is too short"
            PasswordReason.NOT_CONTAINS_NUM -> "Password must contain at least one digit"
        }
    }
}
