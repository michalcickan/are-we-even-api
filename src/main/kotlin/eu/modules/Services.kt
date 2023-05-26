package eu.modules

import eu.services.UserService
import eu.services.UserServiceImpl
import org.koin.dsl.module

val servicesModule = module {
    factory<UserService> { UserServiceImpl(get()) }
}
