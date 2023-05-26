package eu.services

import eu.models.parameters.UserParameters
import eu.models.responses.Address
import eu.models.responses.User
import eu.modules.TransactionHandler
import eu.tables.AddressDAO
import eu.tables.UserDAO

interface UserService {
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: Long): User
    suspend fun createUser(params: UserParameters): User
}

class UserServiceImpl(val transactionHandler: TransactionHandler) : UserService {
    override suspend fun getUsers(): List<User> {
        return transactionHandler.perform {
            val test = UserDAO.all().map { it.toUser() }
            test
        }
    }

    override suspend fun createUser(params: UserParameters): User {
        return transactionHandler.perform {
            UserDAO.new {
                name = params.name
                surname = params.surname
                middleName = params.middleName
            }.toUser()
        }
    }

    override suspend fun getUser(id: Long): User {
        return transactionHandler.perform {
            UserDAO[id].toUser()
        }
    }
}

fun UserDAO.toUser() = User(
    id.value,
    name,
    middleName,
    surname,
    addresses.map { it.toAddress() },
)

fun AddressDAO.toAddress() = Address(
    id.value,
    street,
    zip,
    city,
    country,
)
