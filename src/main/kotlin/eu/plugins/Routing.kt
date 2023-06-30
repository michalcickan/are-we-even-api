package eu.plugins

import eu.routes.authRoutes
import eu.services.UserService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.lettuce.core.api.sync.RedisStringCommands
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val redis by inject<RedisStringCommands<String, String>>()
    val userService by inject<UserService>()

    routing {
        authRoutes()
        get("/user/{id}") {
            val id = call.parameters["id"]!!.toLong()

            call.respond(userService.getUser(id))
        }

        get("/users") {
            call.respond(userService.getUsers())
        }
    }
}
