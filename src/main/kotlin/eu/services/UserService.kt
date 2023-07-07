package eu.services

import LoginType
import LoginTypeDao
import LoginTypeTable
import eu.models.responses.AccessToken
import eu.models.responses.User
import eu.models.responses.toAccessToken
import eu.models.responses.toUser
import eu.modules.ITransactionHandler
import eu.tables.AccessTokenDAO
import eu.tables.UserDAO
import java.time.LocalDateTime

interface UserService {
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: Long): User
    suspend fun createUser(
        email: String,
        name: String,
        middleName: String?,
        surname: String,
    ): User

    suspend fun createAccessToken(
        platformSpecificToken: String,
        loginType: LoginType,
        userId: Long,
        expiryDate: LocalDateTime?,
    ): AccessToken
}

class UserServiceImpl(private val transactionHandler: ITransactionHandler, private val jwtService: IJWTService) :
    UserService {
    override suspend fun getUsers(): List<User> {
        return transactionHandler.perform {
            val test = UserDAO.all().map { it.toUser() }
            test
        }
    }

    override suspend fun createUser(
        email: String,
        name: String,
        middleName: String?,
        surname: String,
    ): User {
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

    override suspend fun createAccessToken(
        platformSpecificToken: String,
        loginType: LoginType,
        userId: Long,
        expiryDate: LocalDateTime?,
    ): AccessToken {
        return transactionHandler.perform {
            AccessTokenDAO.new {
                this.platformAgnosticToken = platformSpecificToken
                this.accessToken = jwtService.generateToken(userId)
                this.loginType = LoginTypeDao.find { LoginTypeTable.loginType eq loginType }.first()
                this.user = UserDAO[userId]
                this.expiryDate = expiryDate ?: LocalDateTime.now()
            }.toAccessToken()
        }
    }
}
