package eu.modules

import io.ktor.server.application.*
import org.koin.core.module.Module
import org.koin.dsl.module

fun environmentModule(environment: ApplicationEnvironment): Module {
    return module {
        single {
            environment
        }
    }
}
