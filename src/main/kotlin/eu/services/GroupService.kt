package eu.services

import eu.exceptions.APIException
import eu.models.parameters.CreateGroupParameters
import eu.models.responses.Debt
import eu.models.responses.Group
import eu.models.responses.toDebt
import eu.models.responses.toGroup
import eu.models.responses.users.User
import eu.models.responses.users.toSimpleUser
import eu.modules.ITransactionHandler
import eu.tables.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

interface IGroupService {
    suspend fun createGroup(params: CreateGroupParameters, creatorUserId: Long): Group
    suspend fun resolveInvitationToGroup(userId: Long, invitationId: Int, accepted: Boolean)

    suspend fun inviteUserToGroup(groupId: Int, userId: Long)
    suspend fun getGroupsForUser(userId: Long): List<Group>
    suspend fun deleteGroup(groupId: Int)
    suspend fun getGroupDetail(groupId: Int): Group

    suspend fun getDefaultGroup(userId: Long): Group
    suspend fun setDefaultGroup(groupId: Int, userId: Long)

    suspend fun getMembers(groupId: Int): List<User>
    suspend fun getDebts(groupId: Int): List<Debt>
}

class GroupService(
    private val transactionHandler: ITransactionHandler,
    private val invitationService: IInvitationService,
) : IGroupService {
    override suspend fun createGroup(params: CreateGroupParameters, creatorUserId: Long): Group {
        return transactionHandler.perform {
            val group = createDAOGroup(params, creatorUserId)
            assignGroupToUser(group, creatorUserId, false)
            group.toGroup(false)
        }
    }

    override suspend fun resolveInvitationToGroup(userId: Long, invitationId: Int, accepted: Boolean) {
        val invitation = invitationService.handleInvitation(invitationId, accepted)
        val groupId = invitation.group.id
        if (accepted) {
            this.checkIfUserInGroup(groupId, userId)
            transactionHandler.perform {
                assignGroupToUser(GroupDAO[groupId], userId, false)
            }
        }
    }

    override suspend fun inviteUserToGroup(groupId: Int, userId: Long) {
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
                        it[Groups.id].value,
                        it[Groups.name],
                        it[UsersGroups.usersWorkingGroup],
                    )
                }
        }
    }

    override suspend fun deleteGroup(groupId: Int) {
        return transactionHandler.perform {
            val group = GroupDAO[groupId]
            UserGroupDAO
                .find { UsersGroups.groupId eq group.id }
                .forEach { it.delete() }
            group.delete()
        }
    }

    override suspend fun getGroupDetail(groupId: Int): Group {
        return transactionHandler.perform {
            val users = UsersGroups
                .innerJoin(Users)
                .select { UsersGroups.groupId eq groupId }
                .map { it.toSimpleUser() }
            GroupDAO[groupId].toGroup(users = users)
        }
    }

    override suspend fun getDefaultGroup(userId: Long): Group {
        return transactionHandler.perform {
            val userGroups = UserGroupDAO
                .find { UsersGroups.userId eq userId }
            if (userGroups.empty()) {
                val group = createDAOGroup(CreateGroupParameters(name = "Main group"), userId)
                assignGroupToUser(group, userId, true)
                group.toGroup(true)
            } else {
                val defaultUserGroup: UserGroupDAO? = userGroups
                    .firstNotNullOfOrNull { if (it.usersWorkingGroup) it else null }
                if (defaultUserGroup != null) {
                    defaultUserGroup.group
                } else {
                    val firstUserGroup = userGroups.first()
                    firstUserGroup.usersWorkingGroup = true
                    firstUserGroup.group
                }.toGroup(true)
            }
        }
    }

    override suspend fun getDebts(groupId: Int): List<Debt> {
        return transactionHandler.perform {
            DebtorDAO.find { Debtors.groupId eq groupId }
                .map { it.toDebt() }
        }
    }

    override suspend fun setDefaultGroup(groupId: Int, userId: Long) {
        return transactionHandler.perform {
            UserGroupDAO
                .find {
                    (UsersGroups.userId eq userId)
                }
                .forEach {
                    it.usersWorkingGroup = it.group.id.value == groupId
                }
        }
    }

    override suspend fun getMembers(groupId: Int): List<User> {
        return transactionHandler.perform {
            UsersGroups.innerJoin(Users)
                .select { UsersGroups.groupId eq groupId }
                .map {
                    it.toSimpleUser()
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

    private fun createDAOGroup(params: CreateGroupParameters, creatorUserId: Long): GroupDAO {
        return GroupDAO.new {
            this.name = params.name
            this.createdBy = UserDAO[creatorUserId]
        }
    }

    private fun assignGroupToUser(group: GroupDAO, creatorUserId: Long, isDefault: Boolean): UserGroupDAO {
        return UserGroupDAO.new {
            this.group = group
            this.user = UserDAO[creatorUserId]
            this.usersWorkingGroup = isDefault
        }
    }
}
