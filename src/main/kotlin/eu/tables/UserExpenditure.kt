package eu.tables

import eu.tables.OweDAO.Companion.referrersOn
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object UserExpenditure : IntIdTable() {
    val userId = reference("userId", Users)
    val expenditureId = reference("expenditureId", Expenditures)
    val paidAmount = float("paidAmount")
}

class UserExpenditureDAO(id: EntityID<Int>) : IntEntity(id) {
    val expenditureID by ExpenditureDAO referrersOn UserExpenditure.expenditureId
    val payerUser by UserDAO referencedOn UserExpenditure.userId
    val paidAmount by UserExpenditure.paidAmount
}
