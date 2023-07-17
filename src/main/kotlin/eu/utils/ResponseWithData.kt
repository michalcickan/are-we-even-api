import eu.models.responses.GenericResponse
import eu.utils.APIException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*

suspend inline fun <reified T : Any> ApplicationCall.respondWithData(data: T?) {
    respond(HttpStatusCode.OK, GenericResponse<T>(data, null))
}

suspend inline fun ApplicationCall.respondWithError(statusCode: HttpStatusCode, message: String) {
    respond(statusCode, GenericResponse.createError<Unit>(message))
}

suspend inline fun <reified T : Any> handleRequest(call: ApplicationCall, block: suspend () -> T) {
    try {
        call.respondWithData(block())
    } catch (e: APIException) {
        call.respondWithError(e.statusCode, e.message)
    } catch (e: ContentTransformationException) {
        call.respondWithError(HttpStatusCode.BadRequest, "Invalid request body ${e.message}")
    }
}
