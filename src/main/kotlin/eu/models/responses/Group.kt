package eu.models.responses

import eu.models.responses.users.User
import eu.tables.GroupDAO
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val isDefault: Boolean?,
    val members: List<User>? = null,
)

fun GroupDAO.toGroup(isDefault: Boolean? = null, users: List<User>? = null): Group {
    return Group(
        id.value,
        name,
        isDefault,
        users,
    )
}
