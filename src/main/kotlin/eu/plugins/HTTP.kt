package eu.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*

fun Application.configureHTTP() {
    routing {
//        swaggerUI(path = "openapi")
    }
}
