package eu.plugins

import eu.models.responses.PagedData
import eu.models.responses.users.User
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false
                serializersModule = SerializersModule {
                    contextual(PagedData.serializer(User.serializer()))
                }
            },
        )
    }
}
