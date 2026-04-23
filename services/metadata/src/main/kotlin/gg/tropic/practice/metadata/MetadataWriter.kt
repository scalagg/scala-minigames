package gg.tropic.practice.metadata

import io.lettuce.core.api.StatefulRedisConnection
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MetadataWriter
{
    private val updates = mutableMapOf<String, String>()
    private val bulk = mutableMapOf<String, String>()
    private val updateLock = ReentrantLock()

    private var namespace = "tropicpractice"
    private lateinit var connection: StatefulRedisConnection<String, String>

    fun withNamespace(namespace: String) = apply {
        this.namespace = namespace
    }

    fun withConnection(connection: StatefulRedisConnection<String, String>) = apply {
        this.connection = connection
    }

    fun write(key: String, value: String)
    {
        updateLock.withLock {
            updates[key] = value
        }
    }

    fun enableAutomatedPush()
    {
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate(::push, 0L, 500L, TimeUnit.MILLISECONDS)
    }

    fun push()
    {
        updateLock.withLock {
            bulk.putAll(updates)
            updates.clear()

            connection.sync()
                .setex(
                    "$namespace:metadata:bulk",
                    10,
                    Serializers.gson.toJson(BulkMetadata(bulk))
                )
        }
    }
}
