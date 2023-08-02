package eu.tables

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object UsersPasswords : LongIdTable() {
    val userId = reference("userId", Users).uniqueIndex()
    val password = varchar("password", 1000)
}

class UserPasswordDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserPasswordDAO>(UsersPasswords)

    var userId by UserDAO referencedOn UsersPasswords.userId
    var password by UsersPasswords.password
}
