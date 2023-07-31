package eu.plugins

import eu.models.responses.GenericResponse
import eu.validation.IAuthRequestValidation
import eu.validation.IExpenditureRequestValidation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.koin.ktor.ext.inject

fun Application.configureRequestValidation() {
    val authValidation by inject<IAuthRequestValidation>()
    val expenditureValidationService by inject<IExpenditureRequestValidation>()

    install(RequestValidation) {
        validate(authValidation::loginParameters)

        validate(authValidation::registrationParameters)

        validate(expenditureValidationService::addExpenditure)
        validate(expenditureValidationService::updateExpenditure)
    }

    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                GenericResponse.createError<Unit>(cause.reasons.joinToString()),
            )
        }
    }
}
