package eu.helpers

import io.ktor.server.plugins.requestvalidation.*

fun ValidationResult.getInvalidMessage(): String? {
    return when (this) {
        is ValidationResult.Invalid -> reasons.joinToString(",")
        is ValidationResult.Valid -> null
    }
}
