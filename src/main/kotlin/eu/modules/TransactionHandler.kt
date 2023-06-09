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

interface ITransactionHandler {
    fun createTables()
    suspend fun <T> perform(block: () -> T): T
}

class TransactionHandler(private val environment: ApplicationEnvironment) : ITransactionHandler {
    private val database: Database by lazy {
        val url = environment.config.property("postgres.jdbcURL").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()
        val driver = environment.config.property("postgres.driverClassName").getString()

        Database.connect(
            url,
            driver,
            user,
            password,
        )
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
            TransactionHandler(environment)
        }
    }
}
