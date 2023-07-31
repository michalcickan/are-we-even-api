package eu.exceptions

import io.ktor.http.*

sealed class APIException(val statusCode: HttpStatusCode, override val message: String) : Throwable() {
    object UserDoesNotExist : APIException(HttpStatusCode.UnprocessableEntity, "User does not exist")
    object UserAlreadyExists : APIException(HttpStatusCode.UnprocessableEntity, "User with this email already exists")
    object TokenExpired : APIException(HttpStatusCode.Unauthorized, "Token expired")

    object LoginNotMatch : APIException(HttpStatusCode.UnprocessableEntity, "Password or email don't match")
}
