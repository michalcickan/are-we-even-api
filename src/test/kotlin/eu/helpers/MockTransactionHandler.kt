package eu.helpers

import eu.modules.ITransactionHandler
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class MockTransactionHandler(
    private val databaseName: String = "test_db_${Instant.now().epochSecond}_${UUID.randomUUID()}",
) : ITransactionHandler {
    @Suppress("MemberVisibilityCanBePrivate")
    protected val database: Database by lazy {
        Database.connect("jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1;IGNORECASE=true;")
    }

    fun createTables(tables: Array<Table>) {
        transaction(database) {
            SchemaUtils.drop(*tables)
            SchemaUtils.create(*tables)
        }
    }

    fun databaseTearDown() = TransactionManager.closeAndUnregister(database)

    override suspend fun <T> perform(block: () -> T): T {
        return transaction(database) {
            block()
        }
    }

    override fun syncPerform(block: () -> Unit) {
        return transaction(database) {
            block()
        }
    }
}
