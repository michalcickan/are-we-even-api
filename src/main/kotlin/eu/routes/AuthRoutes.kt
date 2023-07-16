package eu.routes

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import eu.services.IAuthService
import eu.services.LoginParameters
import eu.utils.APIException
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

val env = dotenv()

val transport = NetHttpTransport()
val jsonFactory = GsonFactory()

fun Route.authRoutes() {
    val authService by inject<IAuthService>()

    post("/login") {
        try {
            val loginParameters = call.receive<LoginParameters>()
            call.respond(HttpStatusCode.OK, authService.loginWith(loginParameters))
        } catch (e: APIException) {
            call.respond(e.statusCode, e.message)
        } catch (e: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    post("/register") {
        try {
            val loginParameters = call.receive<LoginParameters>()
            call.respond(HttpStatusCode.OK, authService.loginWith(loginParameters))
        } catch (e: APIException) {
            call.respond(e.statusCode, e.message)
        } catch (e: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }
}
