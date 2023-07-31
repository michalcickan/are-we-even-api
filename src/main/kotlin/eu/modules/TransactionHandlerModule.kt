package eu.modules

import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.dsl.module

interface ITransactionHandler {
    suspend fun <T> perform(block: () -> T): T
    fun syncPerform(block: () -> Unit): Unit
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

    override suspend fun <T> perform(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }

    override fun syncPerform(block: () -> Unit) {
        transaction(database) { block() }
    }
}

val transactionHandlerModule = module {
    single<ITransactionHandler> {
        TransactionHandler(get())
    }
}
