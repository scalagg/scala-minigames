package mc.arch.minigames.ugc.gateway

import gg.tropic.practice.ugc.HostedWorldRPC

object HostedWorldGatewayManager
{
    private val worldLoadService = WorldLoadService(HostedWorldRPC.worldLoadRPCService)

    fun load()
    {
        HostedWorldRPC.visitWorldRPCService.addHandler(VisitWorldRequestHandler(worldLoadService))
    }
}
