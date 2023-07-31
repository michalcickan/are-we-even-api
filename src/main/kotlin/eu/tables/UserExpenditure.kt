package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object UserExpenditures : IntIdTable() {
    val userId = reference("userId", Users)
    val expenditureId = reference("expenditureId", Expenditures)
    val paidAmount = float("paidAmount")
    val dueAmount = float("dueAmount")
}

class UserExpenditureDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserExpenditureDAO>(UserExpenditures)

    var user by UserDAO referencedOn UserExpenditures.userId
    var expenditureID by ExpenditureDAO referencedOn UserExpenditures.expenditureId
    var paidAmount by UserExpenditures.paidAmount
    var dueAmount by UserExpenditures.dueAmount
}
