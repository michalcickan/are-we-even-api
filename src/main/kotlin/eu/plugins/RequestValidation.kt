package eu.plugins

import eu.models.responses.GenericResponse
import eu.validation.IAuthRequestValidation
import eu.validation.IExpenseRequestValidation
import eu.validation.IGroupRequestValidation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.koin.ktor.ext.inject

fun Application.configureRequestValidation() {
    val authValidation by inject<IAuthRequestValidation>()
    val expenseValidationService by inject<IExpenseRequestValidation>()
    val groupRequestValidation by inject<IGroupRequestValidation>()

    install(RequestValidation) {
        validate(authValidation::loginParameters)

        validate(authValidation::registrationParameters)

        validate(expenseValidationService::addExpense)
        validate(expenseValidationService::updateExpense)
        validate(groupRequestValidation::createGroup)
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
