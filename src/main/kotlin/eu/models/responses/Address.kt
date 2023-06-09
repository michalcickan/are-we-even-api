package eu.models.responses

import eu.services.toUser
import eu.tables.AddressDAO
import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val street: String,
    val zip: String,
    val city: String,
    val country: String,
    val userId: Long,
) {
    companion object {
        fun fromAddressDAO(addressDAO: AddressDAO): Address {
            return Address(
                addressDAO.street,
                addressDAO.zip,
                addressDAO.city,
                addressDAO.country,
                addressDAO.userId.toUser().id,
            )
        }
    }

    fun AddressDAO.toAddress(): Address {
        return Address.fromAddressDAO(this)
    }
}
