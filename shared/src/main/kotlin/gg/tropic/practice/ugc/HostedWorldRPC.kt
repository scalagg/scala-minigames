package gg.tropic.practice.ugc

import gg.tropic.practice.application.api.defaults.DPSRedisService
import gg.tropic.practice.application.api.defaults.createAwareRPCService
import gg.tropic.practice.ugc.generation.WorldLoadRequest
import gg.tropic.practice.ugc.generation.WorldLoadResponse
import gg.tropic.practice.ugc.generation.addplayer.AddPlayerRequest
import gg.tropic.practice.ugc.generation.addplayer.AddPlayerResponse
import gg.tropic.practice.ugc.generation.visits.VisitWorldRequest
import gg.tropic.practice.ugc.generation.visits.VisitWorldResponse

object HostedWorldRPC
{
    private val gateway = DPSRedisService("ugcgateway")

    val worldLoadRPCService = gateway.createAwareRPCService<WorldLoadRequest, WorldLoadResponse>("hostedworlds-load")
    val visitWorldRPCService = gateway.createAwareRPCService<VisitWorldRequest, VisitWorldResponse>("hostedworlds-visit")
    val visitWorldAddPlayerRPCService = gateway.createAwareRPCService<AddPlayerRequest, AddPlayerResponse>("hostedworlds-addplayer")
}
