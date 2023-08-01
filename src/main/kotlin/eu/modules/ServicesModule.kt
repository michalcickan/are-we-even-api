package eu.modules

import eu.routes.env
import eu.services.*
import io.ktor.server.application.*
import org.koin.dsl.module
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

val servicesModule = module {
    factory<IJWTService> {
        val environment = get<ApplicationEnvironment>()
        val audience = environment.config.property("jwt.audience").getString()
        JWTService(get(), env["SECRET"] ?: "", audience, audience)
    }
    factory<PasswordEncoder> { BCryptPasswordEncoder() }
    factory<IUserService> { UserService(get(), get()) }
    factory<IAuthService> { AuthService(get(), get(), get()) }
    factory<IGroupService> { GroupService(get()) }
}
