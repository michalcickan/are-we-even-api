package eu.plugins

import eu.routes.LoginParameters
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<LoginParameters> { params ->
            when {
                params.idToken == null -> ValidationResult.Invalid("Access token must not be null")
                params.loginType == null -> ValidationResult.Invalid("Login type must not be null")
                else -> ValidationResult.Valid
            }
        }
    }
}
