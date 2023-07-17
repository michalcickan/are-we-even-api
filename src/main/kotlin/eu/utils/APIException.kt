package eu.utils

import io.ktor.http.*

sealed class APIException(val statusCode: HttpStatusCode, override val message: String) : Throwable() {
    object InvalidEmailFormat : APIException(HttpStatusCode.BadRequest, "Invalid email format")
    object UserDoesNotExist : APIException(HttpStatusCode.BadRequest, "User does not exist")
    object LoginTypeNeedsIdToken : APIException(HttpStatusCode.BadRequest, "Incorrect login values")

    object LoginNotMatch : APIException(HttpStatusCode.BadRequest, "Password or email don't match")

    object TokenExpired : APIException(HttpStatusCode.BadRequest, "Token expired")
}
