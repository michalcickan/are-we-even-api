package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object UsersGroups : IntIdTable() {
    val userId = reference("userId", Users)
    val groupId = reference("groupId", Groups)
    val isDefault = bool("isDefault")
}

class UserGroupDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserGroupDAO>(UsersGroups)

    var group by GroupDAO referencedOn UsersGroups.groupId
    var user by UserDAO referencedOn UsersGroups.userId
    var isDefault by UsersGroups.isDefault
}
