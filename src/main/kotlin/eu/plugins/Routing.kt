package eu.plugins

import eu.models.parameters.UserParameters
import eu.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.lettuce.core.api.sync.RedisStringCommands
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val redis by inject<RedisStringCommands<String, String>>()
    val userService by inject<UserService>()

    routing {
        get("/") {
            redis.set("first", "test")

            call.respond(
                userService.createUser(
                    UserParameters("michal", null, "cickan"),
                ),
            )
        }
        get("/user/{id}") {
            val id = call.parameters["id"]!!.toLong()

            call.respond(userService.getUser(id))
        }

        get("/users") {
            call.respond(userService.getUsers())
        }
    }
}

/*
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

data class UserParams(val id: Int, val name: String, val surname: String)

fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            // Configure Jackson mapper if needed
        }
    }

    install(StatusPages) {
        exception<ContentTransformationException> { cause ->
            call.respond(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    routing {
        post("/users/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@post
                }

                val userParams = call.receive<UserParams>()
                // Handle the received parameters, including `id`
                // ...

                call.respond(HttpStatusCode.OK, "User created successfully")
            } catch (ex: ContentTransformationException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
            }
        }
    }
}

 */
