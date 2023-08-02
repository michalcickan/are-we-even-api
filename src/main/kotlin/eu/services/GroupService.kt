package eu.services

import eu.exceptions.APIException
import eu.models.parameters.CreateGroupParameters
import eu.models.responses.Group
import eu.models.responses.toGroup
import eu.modules.ITransactionHandler
import eu.tables.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

interface IGroupService {
    suspend fun createGroup(params: CreateGroupParameters, creatorUserId: Long): Group
    suspend fun handleInvitation(groupId: Int, userId: Long, invitationId: Int, accepted: Boolean)

    suspend fun addUserToGroup(groupId: Int, userId: Long)
    suspend fun getGroupsForUser(userId: Long): List<Group>
}

class GroupService(
    private val transactionHandler: ITransactionHandler,
    private val invitationService: IInvitationService,
) : IGroupService {
    override suspend fun createGroup(params: CreateGroupParameters, creatorUserId: Long): Group {
        return transactionHandler.perform {
            val group = GroupDAO.new {
                this.name = params.name
                this.createdBy = UserDAO[creatorUserId]
            }
            UserGroupDAO.new {
                this.group = group
                this.user = UserDAO[creatorUserId]
            }
            group.toGroup()
        }
    }

    override suspend fun handleInvitation(groupId: Int, userId: Long, invitationId: Int, accepted: Boolean) {
        invitationService.handleInvitation(invitationId, accepted)
        if (accepted) {
            this.checkIfUserInGroup(groupId, userId)
            transactionHandler.perform {
                UserGroupDAO.new {
                    this.group = GroupDAO[groupId]
                    this.user = UserDAO[userId]
                }
            }
        }
    }

    override suspend fun addUserToGroup(groupId: Int, userId: Long) {
        this.checkIfUserInGroup(groupId, userId)
        invitationService.makeUserInvitationForGroup(groupId, userId)
    }

    override suspend fun getGroupsForUser(userId: Long): List<Group> {
        return transactionHandler.perform {
            UsersGroups
                .innerJoin(Groups)
                .select { UsersGroups.userId eq userId }
                .map {
                    Group(
                        it[Groups.name],
                        it[Groups.id].value,
                    )
                }
        }
    }

    private suspend fun checkIfUserInGroup(groupId: Int, userId: Long) {
        return transactionHandler.perform {
            if (!UserGroupDAO.find {
                    (UsersGroups.groupId eq groupId) and (UsersGroups.userId eq userId)
                }.empty()
            ) {
                throw APIException.UserAlreadyInGroup
            }
        }
    }
}
