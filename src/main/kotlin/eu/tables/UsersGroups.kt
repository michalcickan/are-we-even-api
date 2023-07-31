package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object UsersGroups : IntIdTable() {
    val userId = reference("userId", Users)
    val groupId = reference("groupId", Groups)
}

class UserGroupDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserGroupDAO>(UsersGroups)

    var groupId by GroupDAO referencedOn UsersGroups.groupId
    var userId by UserDAO referencedOn UsersGroups.userId
}
