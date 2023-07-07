package eu.modules

import eu.routes.env
import eu.services.IJWTService
import eu.services.JWTService
import eu.services.UserService
import eu.services.UserServiceImpl
import org.koin.dsl.module

val servicesModule = module {
    factory<IJWTService> { JWTService(env["SECRET"] ?: "") }
    factory<UserService> { UserServiceImpl(get(), get()) }
}
