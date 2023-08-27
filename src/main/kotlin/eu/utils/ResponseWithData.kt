import eu.exceptions.APIException
import eu.models.responses.GenericResponse
import eu.models.responses.PagedData
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*

suspend inline fun <reified T : Any> ApplicationCall.respondWithData(data: T?) {
    respond(HttpStatusCode.OK, GenericResponse<T>(data, null))
}

suspend inline fun ApplicationCall.respondWithError(statusCode: HttpStatusCode, message: String) {
    respond(statusCode, GenericResponse.createError<Unit>(message))
}

suspend inline fun <reified T : Any> handleRequestWithExceptions(call: ApplicationCall, block: suspend () -> T) {
    try {
        when (val result = block()) {
            is Unit -> {
                call.respond(HttpStatusCode.NoContent)
            }

            is PagedData<*> -> {
                call.respond(HttpStatusCode.OK, result)
            }

            else -> {
                call.respondWithData(result)
            }
        }
    } catch (e: APIException) {
        call.respondWithError(e.statusCode, e.message)
    } catch (e: ContentTransformationException) {
        call.respondWithError(HttpStatusCode.BadRequest, "Invalid request body ${e.message}")
    }
}
