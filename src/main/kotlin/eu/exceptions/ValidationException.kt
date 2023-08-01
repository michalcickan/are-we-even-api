package eu.exceptions

import io.ktor.http.*

sealed class ValidationException(val statusCode: HttpStatusCode, override val message: String) : Throwable() {
    object InvalidEmailFormat : APIException(HttpStatusCode.UnprocessableEntity, "Invalid email format")
    object IncorrectLoginValues : APIException(HttpStatusCode.UnprocessableEntity, "Incorrect login values")

    object NoChange :
        APIException(HttpStatusCode.UnprocessableEntity, "You should provide at least one change you'd like to perform")

    object TotalPaidAndDueAmountsAreNotEqual : APIException(
        HttpStatusCode.UnprocessableEntity,
        "The total paid amount and total due amount do not match",
    )

    object QueryIsMissing : APIException(
        HttpStatusCode.UnprocessableEntity,
        "If you search for a user, `query` parameter is needed",
    )

    object PaidOrDueAmountCannotBeNegative : APIException(
        HttpStatusCode.UnprocessableEntity,
        "Paid or due amount cannot be negative.",
    )
}
