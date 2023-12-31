package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Addresses : IntIdTable() {
    val street = varchar("street", 100)
    val zip = varchar("zip", 10)
    val city = varchar("city", 50)
    val country = varchar("country", 50)
    val user = reference("user", Users)
}

class AddressDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AddressDAO>(Addresses)

    var street by Addresses.street
    var zip by Addresses.zip
    var city by Addresses.city
    var country by Addresses.country
    var user by UserDAO referencedOn Addresses.user
}
