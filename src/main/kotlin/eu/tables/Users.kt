package eu.tables

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Users : LongIdTable() {
    val name = varchar("name", 255)
    val middleName = varchar("middle_name", 255).nullable()
    val surname = varchar("surname", 255)
    val addresses = reference("address", Addresses).nullable()
}

class UserDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserDAO>(Users)

    var name by Users.name
    var middleName by Users.middleName
    var surname by Users.surname
    val addresses by AddressDAO referrersOn Addresses.userId
}
