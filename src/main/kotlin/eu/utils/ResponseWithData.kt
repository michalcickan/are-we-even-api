import eu.exceptions.APIException
import eu.models.responses.GenericResponse
import eu.models.responses.PagedData
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*

suspend inline fun <reified T : Any> ApplicationCall.respondWithData(data: T?) {
    respond(HttpStatusCode.OK, GenericResponse<T>(data, null))
}

suspend inline fun <reified T : Any> ApplicationCall.respondWithPagedData(data: PagedData<T>) {
    respond(HttpStatusCode.OK, data)
}

suspend inline fun ApplicationCall.respondWithError(statusCode: HttpStatusCode, message: String) {
    respond(statusCode, GenericResponse.createError<Unit>(message))
}

suspend inline fun <reified T : Any> responseWithGenericData(
    call: ApplicationCall,
    block: suspend () -> T,
) {
    call.handleRequestBlockWithExceptions {
        when (val result = block()) {
            is Unit -> {
                call.respond(HttpStatusCode.NoContent)
            }
// this doesn't work. Serializer fails, that it didn't find such class
//            is PagedData<*> -> {
//                call.respond(HttpStatusCode.OK, result)
//            }

            else -> {
                call.respondWithData(result)
            }
        }
    }
}

suspend inline fun <reified T : Any> responseWithPagedData(
    call: ApplicationCall,
    block: suspend () -> PagedData<T>,
) {
    call.handleRequestBlockWithExceptions {
        call.respondWithPagedData(block())
    }
}

suspend inline fun <reified T : Any> ApplicationCall.handleRequestBlockWithExceptions(
    tryBlock: suspend () -> T,
) {
    try {
        tryBlock()
    } catch (e: APIException) {
        respondWithError(e.statusCode, e.message)
    } catch (e: ContentTransformationException) {
        respondWithError(HttpStatusCode.BadRequest, "Invalid request body ${e.message}")
    }
}
