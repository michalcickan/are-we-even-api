package eu.models.responses

import eu.tables.GroupDAO
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val isDefault: Boolean?,
)

fun GroupDAO.toGroup(isDefault: Boolean? = null): Group {
    return Group(id.value, name, isDefault)
}
