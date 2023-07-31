package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Groups : IntIdTable() {
    val name = varchar("name", 1000)
    val createdBy = reference("userId", Users)
}

class GroupDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GroupDAO>(Groups)

    var name by Groups.name
    var createdBy by UserDAO referencedOn Groups.createdBy
}
