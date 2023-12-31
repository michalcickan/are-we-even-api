package eu.plugins

import eu.modules.*
import io.ktor.server.application.* // ktlint-disable no-wildcard-imports
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection() {
    install(Koin) {
        modules(
            environmentModule(environment),
            redisModule,
            transactionHandlerModule,
            servicesModule,
            validationModule,
        )
    }
}
