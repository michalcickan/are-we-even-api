package eu

import eu.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module(testing: Boolean = false) {
    configureSerialization()
    if (!testing) {
        configureDependencyInjection()
    }
    configureRequestValidation()
    configureHTTP()
    configureAuthentication(testing)
    configureRouting()
    configureDatabases()
}
