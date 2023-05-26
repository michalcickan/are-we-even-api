package eu.modules

import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisStringCommands
import org.koin.dsl.module

val redisModule = module {
    single<RedisStringCommands<String, String>> {
        val redisClient = RedisClient.create("redis://localhost:6379")
        val connection = redisClient.connect()
        connection.sync()
    }
}
