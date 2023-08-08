package eu.utils

import io.ktor.server.application.*

enum class CustomHeaderField(val value: String) {
    DeviceId("X-Device-ID"),
}

fun ApplicationCall.getHeader(headerField: CustomHeaderField): String? {
    return request.headers[headerField.value]
}
