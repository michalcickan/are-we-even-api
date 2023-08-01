package eu.services

import eu.exceptions.APIException
import eu.models.parameters.CreateUserAddressParameters
import eu.models.parameters.UpdateUserParameters
import eu.models.parameters.UserFilterColumn
import eu.models.responses.Address
import eu.models.responses.toAddress
import eu.models.responses.users.User
import eu.models.responses.users.toUser
import eu.modules.ITransactionHandler
import eu.tables.*
import eu.validation.IAuthRequestValidation
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.or
import java.util.*

interface IUserService {
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: Long): User
    suspend fun getUser(email: String): User?
    suspend fun createUser(
        email: String,
        name: String?,
        middleName: String?,
        surname: String?,
    ): User

    suspend fun updateUser(
        userId: Long,
        params: UpdateUserParameters,
    )

    suspend fun storeUserPassword(
        userId: Long,
        password: String,
    )

    suspend fun getPassword(userId: Long): String?

    suspend fun addAddress(userId: Long, parameters: CreateUserAddressParameters): Address

    suspend fun searchUsers(query: String, filterCol: UserFilterColumn?): List<User>
}

class UserService(
    private val transactionHandler: ITransactionHandler,
    private val validationService: IAuthRequestValidation,
) :
    IUserService {
    override suspend fun getUsers(): List<User> {
        return transactionHandler.perform {
            val test = UserDAO.all().map { it.toUser() }
            test
        }
    }

    override suspend fun createUser(
        email: String,
        name: String?,
        middleName: String?,
        surname: String?,
    ): User {
        return transactionHandler.perform {
            if (!UserDAO.find { Users.email eq email }.empty()) {
                throw APIException.UserAlreadyExists
            }
            UserDAO.new {
                this.email = email
                this.name = name
                this.middleName = middleName
                this.surname = surname
            }.toUser()
        }
    }

    override suspend fun getUser(id: Long): User {
        return transactionHandler.perform {
            val addresses = AddressDAO.find { Addresses.user eq id }.toList()
            UserDAO[id].toUser(addresses)
        }
    }

    override suspend fun updateUser(
        userId: Long,
        params: UpdateUserParameters,
    ) {
        return transactionHandler.perform {
            val user = UserDAO.findById(userId) ?: throw APIException.UserDoesNotExist
            user.name = params.name ?: user.name
            user.surname = params.surname ?: user.surname
            user.middleName = params.middleName ?: user.middleName
            user.email = params.email ?: user.email
        }
    }

    override suspend fun storeUserPassword(
        userId: Long,
        password: String,
    ) {
        return transactionHandler.perform {
            UserPasswordDAO.new {
                this.userId = UserDAO[userId]
                this.password = password
            }
        }
    }

    override suspend fun getUser(email: String): User? {
        return transactionHandler.perform {
            UserDAO.find { Users.email eq email }.singleOrNull()?.toUser()
        }
    }

    override suspend fun getPassword(userId: Long): String? {
        return transactionHandler.perform {
            UserPasswordDAO.find { UserPasswords.userId eq userId }.singleOrNull()?.password
        }
    }

    override suspend fun addAddress(userId: Long, params: CreateUserAddressParameters): Address {
        return transactionHandler.perform {
            AddressDAO.new {
                this.city = params.city
                this.user = UserDAO[userId]
                this.zip = params.zip
                this.street = params.street
                this.country = params.country
            }.toAddress()
        }
    }

    override suspend fun searchUsers(query: String, filterCol: UserFilterColumn?): List<User> {
        val lowerQuery = "%${query.lowercase(Locale.getDefault())}%"
        return transactionHandler.perform {
            if (filterCol != null) {
                searchForUsersWithFilterColumn(lowerQuery, filterCol!!)
            } else {
                searchFulltextForUsersWithQuery(lowerQuery)
            }.map { it.toUser() }
        }
    }

    private fun searchFulltextForUsersWithQuery(query: String): SizedIterable<UserDAO> {
        return UserDAO.find {
            (Users.name like query) or
                (Users.email like query) or
                (Users.surname like query) or
                (Users.middleName like query)
        }
    }

    private fun searchForUsersWithFilterColumn(query: String, filterCol: UserFilterColumn): SizedIterable<UserDAO> {
        return UserDAO.find {
            when (filterCol) {
                UserFilterColumn.EMAIL -> {
                    Users.email like query
                }

                UserFilterColumn.MIDDLE_NAME -> {
                    Users.middleName like query
                }

                UserFilterColumn.SURNAME -> {
                    Users.surname like query
                }

                else -> {
                    Users.name like query
                }
            }
        }
    }
}
