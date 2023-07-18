package eu.models.responses

import eu.tables.AddressDAO
import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val street: String,
    val zip: String,
    val city: String,
    val country: String,
)

fun AddressDAO.toAddress(): Address {
    return Address(
        this.street,
        this.zip,
        this.city,
        this.country,
    )
}
