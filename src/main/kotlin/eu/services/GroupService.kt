package eu.services

import eu.models.parameters.CreateGroupParameters
import eu.models.responses.Group
import eu.modules.ITransactionHandler
import eu.tables.*
import org.jetbrains.exposed.sql.select

interface IGroupService {
    suspend fun createGroup(params: CreateGroupParameters, creatorUserId: Long)
    suspend fun addUserToGroup(groupId: Int, userId: Long)
    suspend fun getGroupsForUser(userId: Long): List<Group>
}

class GroupService(
    private val transactionHandler: ITransactionHandler,
) : IGroupService {
    override suspend fun createGroup(params: CreateGroupParameters, creatorUserId: Long) {
        transactionHandler.perform {
            GroupDAO.new {
                this.name = params.name
                this.createdBy = UserDAO[creatorUserId]
            }
        }
    }

    override suspend fun addUserToGroup(groupId: Int, userId: Long) {
        transactionHandler.perform {
            UserGroupDAO.new {
                this.groupId = GroupDAO[groupId]
                this.userId = UserDAO[userId]
            }
        }
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
}
