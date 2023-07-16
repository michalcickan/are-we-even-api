package eu.modules

import eu.routes.env
import eu.services.*
import org.koin.dsl.module
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

val servicesModule = module {
    factory<IJWTService> { JWTService(env["SECRET"] ?: "") }
    factory<PasswordEncoder> { BCryptPasswordEncoder() }
    factory<IValidationService> { ValidationService() }
    factory<IUserService> { UserService(get(), get(), get()) }
    factory<IAuthService> { AuthService(get(), get()) }
}
