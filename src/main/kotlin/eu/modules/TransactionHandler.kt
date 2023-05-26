package eu.modules

import eu.tables.Addresses
import eu.tables.Users
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.module.Module
import org.koin.dsl.*
import java.sql.Connection
import java.sql.DriverManager

interface ITransactionHandler {
    fun createTables()
    suspend fun <T> perform(block: () -> T): T
}

class TransactionHandler(private val connection: Connection) : ITransactionHandler {
    private val database: Database by lazy {
        Database.connect({ connection })
    }

    override fun createTables() {
        transaction(database) {
            SchemaUtils.create(Users, Addresses)
        }
    }

    override suspend fun <T> perform(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }
}

fun transactionHandlerModule(environment: ApplicationEnvironment): Module {
    return module {
        single<TransactionHandler> {
            Class.forName(environment.config.property("postgres.driverClassName").getString())

            val url = environment.config.property("postgres.jdbcURL").getString()
            val user = environment.config.property("postgres.user").getString()
            val password = environment.config.property("postgres.password").getString()

            TransactionHandler(DriverManager.getConnection(url, user, password))
        }
    }
}
