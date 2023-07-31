package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Owees : IntIdTable() {
    val payerUserId = reference("payerUserId", Users)
    val oweeUserId = reference("oweeUserId", Users)
    val amountOwed = float("amountOwed")
    val groupId = reference("groupId", Groups)
}

class OweeDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OweeDAO>(Owees)

    var amountOwed by Owees.amountOwed
    var payerUser by UserDAO referencedOn Owees.payerUserId
    var oweeUser by UserDAO referencedOn Owees.oweeUserId
    var groupId by GroupDAO referencedOn Owees.groupId
}
