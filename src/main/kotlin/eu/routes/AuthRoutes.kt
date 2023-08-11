package eu.routes

import LoginType
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import eu.exceptions.ValidationException
import eu.services.IAuthService
import eu.services.IJWTService
import eu.utils.CustomHeaderField
import eu.utils.getHeader
import handleRequestWithExceptions
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

val env = dotenv()

val transport = NetHttpTransport()
val jsonFactory = GsonFactory()

fun Route.authRoutes() {
    val authService by inject<IAuthService>()
    val jwtService by inject<IJWTService>()

    post("/login/{loginType}") {
        handleRequestWithExceptions(call) {
            call.checkForDeviceId()
            val loginType = call.parameters["loginType"]
            val parsedLoginType = if (loginType != null) {
                LoginType.valueOf(loginType.uppercase()) // Parse the string to the enum
            } else {
                // Handle the case when loginType is not provided
                // For example, perform a default action or return an error response
                // Here, I'm assuming a default action of EMAIL login
                LoginType.EMAIL
            }
            authService.loginWith(
                call.receive(),
                parsedLoginType,
                call.deviceId(),
            )
        }
    }

    post("/login") {
        call.checkForDeviceId()
        handleRequestWithExceptions(call) {
            authService.loginWith(
                call.receive(),
                LoginType.EMAIL,
                call.deviceId(),
            )
        }
    }

    post("/register") {
        call.checkForDeviceId()
        handleRequestWithExceptions(call) {
            authService.registerWith(call.receive(), call.deviceId())
        }
    }

    post("/token") {
        call.checkForDeviceId()
        handleRequestWithExceptions(call) {
            authService.recreateAccessToken(call.receive(), call.deviceId())
        }
    }

    authenticate("auth-jwt") {
        post("/logout") {
            handleRequestWithExceptions(call) {
                call.checkForDeviceId()
                authService.logout(
                    jwtService.getUserIdFromPrincipalPayload(
                        call.principal(),
                    ),
                    call.deviceId(),
                )
            }
        }
    }
}

private fun ApplicationCall.deviceId(): String {
    return getHeader(CustomHeaderField.DeviceId) ?: ""
}

private fun ApplicationCall.checkForDeviceId() {
    if (getHeader(CustomHeaderField.DeviceId) == null) {
        throw ValidationException.DeviceIdMissing
    }
}
