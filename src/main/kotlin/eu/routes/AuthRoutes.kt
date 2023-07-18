package eu.routes

import LoginType
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import eu.services.IAuthService
import handleRequestWithExceptions
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

    post("/login/{loginType}") {
        handleRequestWithExceptions(call) {
            val loginType = call.parameters["loginType"]
            val parsedLoginType = if (loginType != null) {
                LoginType.valueOf(loginType.uppercase()) // Parse the string to the enum
            } else {
                // Handle the case when loginType is not provided
                // For example, perform a default action or return an error response
                // Here, I'm assuming a default action of EMAIL login
                LoginType.EMAIL
            }
            authService.loginWith(call.receive(), parsedLoginType)
        }
    }

    post("/login") {
        handleRequestWithExceptions(call) {
            authService.loginWith(call.receive(), LoginType.EMAIL)
        }
    }

    post("/register") {
        handleRequestWithExceptions(call) {
            authService.registerWith(call.receive())
        }
    }

    post("/token") {
        handleRequestWithExceptions(call) {
            authService.recreateAccessToken(call.receive())
        }
    }
}
