package eu.models.responses

import com.typesafe.config.Optional
import kotlinx.serialization.Serializable

@Serializable
data class GenericResponse<T>(@Optional val data: T?, @Optional val error: APIError?) {
    companion object {
        fun <T> createError(message: String): GenericResponse<T> {
            val error = APIError(message)
            return GenericResponse(null, error)
        }
    }
}
