package eu.tables
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Owes : IntIdTable() {
    val payerUserId = reference("payerUserId", Users)
    val oweeUserId = reference("oweeUserId", Users)
    val expenditureId = reference("expenditureId", Expenditures)
    val amountOwed = float("amountOwed")
}

class OweDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AddressDAO>(Addresses)

    val amountOwed by Owes.amountOwed
    val expenditure by ExpenditureDAO referrersOn Owes.expenditureId
    var payerUser by UserDAO referencedOn Owes.payerUserId
    var oweeUser by UserDAO referencedOn Owes.oweeUserId
}
