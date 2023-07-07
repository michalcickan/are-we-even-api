package eu.tables

import LoginTypeDao
import LoginTypeTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object AccessTokens : IntIdTable() {
    val userId = reference("userId", Users)

    // service-based token.
    val platformAgnosticToken = varchar("platformAgnosticToken", 2500)
    val accessToken = varchar("accessToken", 2500)
    val expiryDate = datetime("expiryDate").defaultExpression(CurrentDateTime)
    val loginType = reference("loginType", LoginTypeTable)
}

class AccessTokenDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AccessTokenDAO>(AccessTokens)

    var platformAgnosticToken by AccessTokens.platformAgnosticToken
    var user by UserDAO referencedOn AccessTokens.userId
    var accessToken by AccessTokens.accessToken
    var expiryDate by AccessTokens.expiryDate
    var loginType by LoginTypeDao referencedOn AccessTokens.loginType
}
