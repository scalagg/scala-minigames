package gg.tropic.practice.games.manager

import com.github.benmanes.caffeine.cache.Caffeine
import gg.tropic.practice.application.api.defaults.addFunctionalHandler
import gg.tropic.practice.games.SystemBulkMetadata
import gg.tropic.practice.metadata.InstanceMetadata
import gg.tropic.practice.metadata.MetadataRPC
import gg.tropic.practice.namespace
import gg.tropic.practice.persistence.RedisShared
import gg.tropic.practice.suffixWhenDev
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object GameManager
{
    private val gameListingCache = Caffeine.newBuilder()
        .expireAfterWrite(2L, TimeUnit.SECONDS)
        .removalListener<String, InstanceMetadata> { _, _, _ -> syncStatusIndexes() }
        .build<String, InstanceMetadata>()

    fun allGames() = gameListingCache.asMap().values
        .flatMap(InstanceMetadata::games)

    fun allHostedWorldInstances() = gameListingCache.asMap().values
        .flatMap(InstanceMetadata::hostedWorldInstanceReferences)

    fun instances() = gameListingCache.asMap()

    private fun syncStatusIndexes()
    {
        RedisShared.keyValueCache.sync().setex(
            "${namespace().suffixWhenDev()}:gamemanager:bulk-statuses",
            10,
            Serializers.gson.toJson(
                SystemBulkMetadata(gameListingCache.asMap())
            )
        )
    }

    fun load()
    {
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate(::syncStatusIndexes, 0L, 500L, TimeUnit.MILLISECONDS)

        MetadataRPC.statusService.addFunctionalHandler { metadata, context ->
            gameListingCache.put(metadata.server, metadata.metadata)

        }
    }
}
