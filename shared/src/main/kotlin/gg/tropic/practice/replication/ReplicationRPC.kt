package gg.tropic.practice.replication

import gg.tropic.practice.application.api.defaults.DPSRedisService
import gg.tropic.practice.application.api.defaults.createAwareRPCService
import gg.tropic.practice.metadata.EmptyResponse
import gg.tropic.practice.replication.generation.rpc.GenerationRequest
import gg.tropic.practice.replication.generation.rpc.GenerationResult

/**
 * @author Subham
 * @since 7/20/25
 */
object ReplicationRPC
{
    private val gateway = DPSRedisService("replications")
    val generationService = gateway.createAwareRPCService<GenerationRequest, GenerationResult>(
        serviceName = "generate",
        timeoutSeconds = 5L
    )

    val statusService = gateway.createAwareRPCService<ReplicationServerStatus, EmptyResponse>(
        serviceName = "status",
        timeoutSeconds = 1L
    )

    fun updateStatus(server: String, status: ServerAvailableReplicationState)
    {
        statusService.call(ReplicationServerStatus(server = server, status = status))
    }
}
