package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Debtors : IntIdTable() {
    val creditorId = reference("creditorId", Users)
    val debtorId = reference("debtorId", Users)
    val amountOwed = float("amountOwed")
    val groupId = reference("groupId", Groups)
}

class DebtorDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DebtorDAO>(Debtors)

    var amountOwed by Debtors.amountOwed
    var creditor by UserDAO referencedOn Debtors.creditorId
    var debtor by UserDAO referencedOn Debtors.debtorId
    var groupId by GroupDAO referencedOn Debtors.groupId
}
