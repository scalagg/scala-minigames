package gg.tropic.practice.metadata

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.games.SystemBulkMetadata
import gg.tropic.practice.games.GameReference
import gg.tropic.practice.namespace
import gg.tropic.practice.persistence.RedisShared
import gg.tropic.practice.suffixWhenDev
import gg.tropic.practice.ugc.HostedWorldInstanceReference
import me.lucko.helper.Schedulers
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.services.CommonsServiceExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service(priority = 1600)
object SystemMetadataService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    fun bindToStatusService(statusService: () -> InstanceMetadata)
    {
        Schedulers
            .async()
            .runRepeating(Runnable {
                MetadataRPC.updateStatus(
                    server = ServerSync.local.id,
                    status = statusService()
                )
            }, 0L, 10L)
            .bindWith(plugin)

        plugin.logger.info("Bound status service. Status updates for available games will be pushed to the gamemanager channel ever 0.25 seconds.")
    }

    private var gameStatusWriteLock = ReentrantReadWriteLock()
    private var recentGameStatuses = listOf<GameReference>()
    private var recentHostedWorldInstancesStatuses = listOf<HostedWorldInstanceReference>()

    fun allGames() = gameStatusWriteLock.read { recentGameStatuses }
    fun allHostedWorldInstances() = gameStatusWriteLock.read { recentHostedWorldInstancesStatuses }

    private fun computeAllGameStatuses() = CompletableFuture
        .supplyAsync {
            RedisShared.keyValueCache
                .sync()
                .get("${namespace().suffixWhenDev()}:gamemanager:bulk-statuses")
                ?.let {
                    Serializers.gson.fromJson(
                        it, SystemBulkMetadata::class.java
                    )
                }
                ?.indexes?.values
        }

    fun getQueued(queueId: String) = Metadata.reader()
        .read("queue:users-queued:$queueId")
        ?.toInt() ?: 0

    fun getPlaying(queueId: String) = Metadata.reader()
        .read("queue:users-playing:$queueId")
        ?.toInt() ?: 0

    fun getTotalRankedQueuePlayers() = Metadata.reader()
        .read("duel:queue-total:ranked")
        ?.toIntOrNull() ?: 0

    fun getTotalUnrankedQueuePlayers() = Metadata.reader()
        .read("duel:queue-total:unranked")
        ?.toIntOrNull() ?: 0

    fun getActiveRankedMatches() = Metadata.reader()
        .read("duel:matches-total:ranked")
        ?.toIntOrNull() ?: 0

    fun getActiveUnrankedMatches() = Metadata.reader()
        .read("duel:matches-total:unranked")
        ?.toIntOrNull() ?: 0

    @Configure
    fun configure()
    {
        Metadata.reader()
            .withNamespace(namespace())
            .withConnection(RedisShared.keyValueCache)
            .enableAutomatedPull(CommonsServiceExecutor)

        CommonsServiceExecutor.scheduleAtFixedRate({
            gameStatusWriteLock.write {
                val statuses = computeAllGameStatuses().join()
                    ?: return@write run {
                        recentGameStatuses = listOf()
                        recentHostedWorldInstancesStatuses = listOf()
                    }

                recentGameStatuses = statuses
                    .flatMap { it.games }
                recentHostedWorldInstancesStatuses = statuses
                    .flatMap { it.hostedWorldInstanceReferences }
            }
        }, 0L, 500L, TimeUnit.MILLISECONDS)
    }
}
