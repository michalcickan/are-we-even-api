package eu.models.responses

import eu.tables.GroupDAO
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
)

fun GroupDAO.toGroup(): Group {
    return Group(id.value, name)
}
