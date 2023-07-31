package eu.routes

import eu.services.IJWTService
import eu.services.IUserService
import handleRequestWithExceptions
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val userService by inject<IUserService>()
    val jwtService by inject<IJWTService>()

    authenticate("auth-jwt") {
        put("/user") {
            handleRequestWithExceptions(call) {
                userService.updateUser(
                    userId = call.userId(jwtService),
                    params = call.receive(),
                )
            }
        }

        get("/user") {
            handleRequestWithExceptions(call) {
                userService.getUser(call.userId(jwtService))
            }
        }

        post("user/address") {
            handleRequestWithExceptions(call) {
                userService.addAddress(
                    call.userId(jwtService),
                    call.receive(),
                )
            }
        }
    }
}

private fun ApplicationCall.userId(jwtService: IJWTService): Long {
    return jwtService.getUserIdFromPrincipalPayload(principal())
}