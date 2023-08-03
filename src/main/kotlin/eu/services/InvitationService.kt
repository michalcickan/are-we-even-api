package eu.services

import eu.exceptions.APIException
import eu.models.responses.Invitation
import eu.models.responses.toInvitation
import eu.modules.ITransactionHandler
import eu.tables.GroupDAO
import eu.tables.InvitationDAO
import eu.tables.Invitations
import eu.tables.UserDAO
import org.jetbrains.exposed.sql.and

interface IInvitationService {
    suspend fun makeUserInvitationForGroup(groupId: Int, userId: Long): Invitation
    suspend fun handleInvitation(invitationId: Int, accept: Boolean): Invitation

    suspend fun getInvitations(userId: Long): List<Invitation>
}

class InvitationService(
    val transactionHandler: ITransactionHandler,
) : IInvitationService {
    override suspend fun makeUserInvitationForGroup(groupId: Int, userId: Long): Invitation {
        return transactionHandler.perform {
            if (isInvitationPending(groupId, userId)) {
                throw APIException.UserAlreadyInvited
            }
            InvitationDAO.new {
                this.group = GroupDAO[groupId]
                this.user = UserDAO[userId]
                this.accepted = null
            }
                .toInvitation()
        }
    }

    override suspend fun handleInvitation(invitationId: Int, accepted: Boolean): Invitation {
        return transactionHandler.perform {
            val invitation = InvitationDAO[invitationId]
            invitation.accepted = accepted
            invitation.toInvitation()
        }
    }

    override suspend fun getInvitations(userId: Long): List<Invitation> {
        return transactionHandler.perform {
            InvitationDAO
                .find { (Invitations.userId eq userId) and (Invitations.accepted eq null) }
                .map { it.toInvitation() }
        }
    }

    private fun isInvitationPending(groupId: Int, userId: Long): Boolean {
        return !InvitationDAO
            .find {
                (Invitations.userId eq userId) and
                    (Invitations.groupId eq groupId) and
                    (Invitations.accepted eq null)
            }
            .empty()
    }
}
