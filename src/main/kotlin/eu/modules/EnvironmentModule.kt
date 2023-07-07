package eu.modules

import io.ktor.server.application.*
import org.koin.dsl.module

val environmentModule = module {
    single<ApplicationEnvironment> { get<Application>().environment }
}
