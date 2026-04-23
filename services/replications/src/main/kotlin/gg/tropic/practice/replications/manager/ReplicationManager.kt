package gg.tropic.practice.replications.manager

import com.github.benmanes.caffeine.cache.Caffeine
import gg.tropic.practice.expectation.GameExpectation
import gg.tropic.practice.replication.*
import gg.tropic.practice.replication.generation.rpc.GenerationRequest
import gg.tropic.practice.replication.generation.rpc.GenerationRequirement
import gg.tropic.practice.replication.generation.rpc.GenerationResult
import gg.tropic.practice.application.api.defaults.addFunctionalHandler
import gg.tropic.practice.metadata.EmptyResponse
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object ReplicationManager
{
    private val gameInstanceCache = Caffeine.newBuilder()
        .expireAfterWrite(2L, TimeUnit.SECONDS)
        .build<String, ReplicationServerStatus>()

    fun allServerStatuses() = gameInstanceCache.asMap()

    fun load()
    {
        ReplicationRPC.statusService.addFunctionalHandler { request, context ->
            gameInstanceCache.put(request.server, request)
            context.reply(EmptyResponse(uniqueId = request.uniqueId))
        }
    }

    fun generateReplication(
        server: String,
        map: String,
        requirement: GenerationRequirement,
        expectation: GameExpectation
    ): CompletableFuture<GenerationResult>
    {
        val request = GenerationRequest(
            server = server,
            map = map,
            requirement = requirement,
            expectation = expectation
        )
        return ReplicationRPC.generationService.call(request).handle { result, throwable ->
            when
            {
                throwable != null -> GenerationResult(
                    uniqueId = request.uniqueId,
                    status = ReplicationResultStatus.FAILED,
                    message = "Service did not get response in time: ${throwable.message}"
                )

                result != null -> result
                else -> GenerationResult(
                    uniqueId = request.uniqueId,
                    status = ReplicationResultStatus.FAILED,
                    message = "System is unavailable (improper backend response)"
                )
            }
        }
    }
}
