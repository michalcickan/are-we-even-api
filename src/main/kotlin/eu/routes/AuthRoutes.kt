package eu.routes

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import eu.services.IAuthService
import handleRequest
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

val env = dotenv()

val transport = NetHttpTransport()
val jsonFactory = GsonFactory()

fun Route.authRoutes() {
    val authService by inject<IAuthService>()

    post("/login") {
        handleRequest(call) {
            authService.loginWith(call.receive())
        }
    }

    post("/register") {
        handleRequest(call) {
            authService.registerWith(call.receive())
        }
    }

    post("/token") {
        handleRequest(call) {
            authService.recreateAccessToken(call.receive())
        }
    }
}
