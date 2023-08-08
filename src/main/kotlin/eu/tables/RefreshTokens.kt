package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object RefreshTokens : IntIdTable() {
    val userId = reference("userId", Users)
    val refreshToken = varchar("refreshToken", 2500)
    val expiryDate = datetime("expiryDate").defaultExpression(CurrentDateTime)
    val deviceId = reference("deviceId", Devices)
}

class RefreshTokenDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RefreshTokenDAO>(RefreshTokens)

    var refreshToken by RefreshTokens.refreshToken
    var expiryDate by RefreshTokens.expiryDate
    var user by UserDAO referencedOn RefreshTokens.userId
    var device by DeviceDAO referencedOn RefreshTokens.deviceId
}
