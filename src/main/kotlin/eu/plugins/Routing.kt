package eu.plugins

import eu.routes.authRoutes
import eu.routes.groupRoutes
import eu.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        authRoutes()
        userRoutes()
        groupRoutes()
    }
}
