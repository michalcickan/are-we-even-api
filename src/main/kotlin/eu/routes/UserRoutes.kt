package eu.routes

import eu.models.parameters.extractUserSearchQueryParameters
import eu.services.IJWTService
import eu.services.IUserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import responseWithGenericData
import responseWithPagedData

fun Route.userRoutes() {
    val userService by inject<IUserService>()
    val jwtService by inject<IJWTService>()

    authenticate("auth-jwt") {
        put("/user") {
            responseWithGenericData(call) {
                userService.updateUser(
                    userId = call.userId(jwtService),
                    params = call.receive(),
                )
            }
        }

        get("/user") {
            responseWithGenericData(call) {
                userService.getUser(call.userId(jwtService))
            }
        }

        post("user/address") {
            responseWithGenericData(call) {
                userService.addAddress(
                    call.userId(jwtService),
                    call.receive(),
                )
            }
        }

        get("/users/search") {
            responseWithPagedData(call) {
                userService.searchUsers(
                    call.extractUserSearchQueryParameters(),
                )
            }
        }
    }
}

private fun ApplicationCall.userId(jwtService: IJWTService): Long {
    return jwtService.getUserIdFromPrincipalPayload(principal())
}
