package eu.models.responses

import eu.models.responses.users.User
import eu.models.responses.users.toSimpleUser
import eu.tables.InvitationDAO
import kotlinx.serialization.Serializable

@Serializable
data class Invitation(
    val id: Int,
    val invitedBy: User,
    val group: Group,
)

fun InvitationDAO.toInvitation(): Invitation {
    return Invitation(
        id.value,
        user.toSimpleUser(),
        group.toGroup(),
    )
}
