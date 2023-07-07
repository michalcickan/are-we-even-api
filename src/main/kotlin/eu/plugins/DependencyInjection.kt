package eu.plugins

import eu.modules.redisModule
import eu.modules.servicesModule
import eu.modules.transactionHandlerModule
import io.ktor.server.application.* // ktlint-disable no-wildcard-imports
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection() {
    install(Koin) {
        // TODO: environment should be also injectable
        modules(redisModule, transactionHandlerModule(environment), servicesModule)
    }
}
