package eu.tables

import LoginTypeDao
import LoginTypeTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Users : LongIdTable() {
    val name = varchar("name", 255)
    val middleName = varchar("middle_name", 255).nullable()
    val surname = varchar("surname", 255)
    val email = varchar("email", 255)
    val loginType = reference("loginType", LoginTypeTable).nullable()
    val token = varchar("token", 1500).nullable()
}

class UserDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserDAO>(Users)

    var name by Users.name
    var middleName by Users.middleName
    var surname by Users.surname
    var email by Users.email
    var loginType by LoginTypeDao optionalReferencedOn Users.loginType
    var token by Users.token
    val addresses by AddressDAO referrersOn Addresses.user
}
