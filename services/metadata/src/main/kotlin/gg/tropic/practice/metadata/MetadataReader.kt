package gg.tropic.practice.metadata

import io.lettuce.core.api.StatefulRedisConnection
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class MetadataReader
{
    private var bulk = mapOf<String, String>()
    private val updateLock = ReentrantReadWriteLock()

    private var namespace = "tropicpractice"
    private lateinit var connection: StatefulRedisConnection<String, String>

    fun withNamespace(namespace: String) = apply {
        this.namespace = namespace
    }

    fun withConnection(connection: StatefulRedisConnection<String, String>) = apply {
        this.connection = connection
    }

    fun read(key: String) = bulk[key]
    fun bulk() = updateLock.read { bulk }

    fun enableAutomatedPull(service: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor())
    {
        service.scheduleAtFixedRate(::pull, 0L, 500L, TimeUnit.MILLISECONDS)
    }

    fun pull()
    {
        updateLock.write {
            connection.sync()
                .get("$namespace:metadata:bulk")
                ?.let {
                    Serializers.gson.fromJson(it, BulkMetadata::class.java)
                }
                ?.let {
                    bulk = it.metadata
                }
        }
    }
}
