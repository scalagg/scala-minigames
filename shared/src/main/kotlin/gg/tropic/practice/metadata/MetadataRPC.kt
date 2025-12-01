package gg.tropic.practice.metadata

import gg.tropic.practice.application.api.defaults.DPSRedisService
import gg.tropic.practice.application.api.defaults.createAwareRPCService

/**
 * @author Subham
 * @since 7/20/25
 */
object MetadataRPC
{
    private val gateway = DPSRedisService("games")
    val statusService = gateway.createAwareRPCService<ServerInstanceMetadata, EmptyResponse>(
        serviceName = "status",
        timeoutSeconds = 1L
    )

    fun updateStatus(server: String, status: InstanceMetadata)
    {
        statusService.call(ServerInstanceMetadata(server = server, metadata = status))
    }
}
