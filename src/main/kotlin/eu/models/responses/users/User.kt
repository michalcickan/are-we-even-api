package eu.models.responses.users

import eu.models.responses.Address
import eu.models.responses.toAddress
import eu.tables.AddressDAO
import eu.tables.UserDAO
import eu.tables.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class User(
    val id: Long,
    val name: String?,
    val middleName: String?,
    val surname: String?,
    val addresses: List<Address>?,
    val email: String,
)

fun UserDAO.toUser(addresses: List<AddressDAO>? = null): User {
    return User(
        id.value,
        name,
        middleName,
        surname,
        addresses?.map { it.toAddress() },
        email,
    )
}

fun UserDAO.toSimpleUser(): User {
    return User(
        id.value,
        name,
        middleName,
        surname,
        null,
        email,
    )
}

fun ResultRow.toSimpleUser(): User {
    return User(
        this[Users.id].value,
        this[Users.name],
        this[Users.middleName],
        this[Users.surname],
        null,
        this[Users.email],
    )
}

/*
https://www.baeldung.com/kotlin/json-convert-data-class

var jsonString = """{"id":1,"description":"Test"}""";
var testModel = gson.fromJson(jsonString, TestModel::class.java)
Assert.assertEquals(testModel.id, 1)
Assert.assertEquals(testModel.description, "Test")
 */
