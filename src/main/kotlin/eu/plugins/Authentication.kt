package eu.plugins

import eu.services.IJWTService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.koin.ktor.ext.inject
import respondWithError

fun Application.configureAuthentication() {
    val jwtService by inject<IJWTService>()
    val myRealm = environment.config.property("jwt.realm").getString()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(jwtService.buildVerifierForToken())
            validate { credential ->
                jwtService.getJWTPrincipal(credential.payload)
            }
            challenge { defaultScheme, realm ->
                call.respondWithError(
                    HttpStatusCode.Unauthorized,
                    "Token is not valid or has expired",
                )
            }
        }
    }
}
