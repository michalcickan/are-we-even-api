package eu.services

import eu.models.responses.User
import eu.models.responses.toUser
import eu.modules.ITransactionHandler
import eu.tables.UserDAO
import eu.tables.UserPasswordDAO
import eu.tables.UserPasswords
import eu.tables.Users
import eu.utils.APIException

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
        email: String?,
        name: String?,
        middleName: String?,
        surname: String?,
    )

    suspend fun createUserPassword(
        userId: Long,
        password: String,
    ): String

    suspend fun getPassword(userId: Long): String?
}

class UserService(
    private val transactionHandler: ITransactionHandler,
    private val jwtService: IJWTService,
    private val validationService: IValidationService,
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
        if (!validationService.validateEmail(email)) {
            throw APIException.InvalidEmailFormat
        }
        return transactionHandler.perform {
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
            UserDAO[id].toUser()
        }
    }

    override suspend fun updateUser(
        userId: Long,
        email: String?,
        name: String?,
        middleName: String?,
        surname: String?,
    ) {
        return transactionHandler.perform {
            val user = UserDAO.findById(userId) ?: throw APIException.UserDoesNotExist
            user.name = name ?: user.name
            user.surname = surname ?: user.surname
            user.middleName = middleName ?: user.middleName
            user.email = email ?: user.email
        }
    }

    override suspend fun createUserPassword(
        userId: Long,
        password: String,
    ): String {
        return transactionHandler.perform {
            UserPasswordDAO.new {
                this.userId = UserDAO[userId]
                this.password = password
            }.password
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
}
