package eu.models.responses

import eu.tables.GroupDAO
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val name: String,
    val id: Int,
)

fun GroupDAO.toGroup(): Group {
    return Group(name, id.value)
}
