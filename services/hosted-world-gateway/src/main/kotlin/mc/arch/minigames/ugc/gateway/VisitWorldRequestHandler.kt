package mc.arch.minigames.ugc.gateway

import gg.scala.commons.consensus.Locks
import gg.tropic.practice.games.manager.GameManager
import gg.tropic.practice.ugc.HostedWorldRPC
import gg.tropic.practice.application.api.defaults.AwareRPCContext
import gg.tropic.practice.application.api.defaults.AwareRPCHandler
import gg.tropic.practice.ugc.generation.WorldLoadStatus
import gg.tropic.practice.ugc.generation.addplayer.AddPlayerRequest
import gg.tropic.practice.ugc.generation.addplayer.AddPlayerStatus
import gg.tropic.practice.ugc.generation.visits.VisitWorldRequest
import gg.tropic.practice.ugc.generation.visits.VisitWorldResponse
import gg.tropic.practice.ugc.generation.visits.VisitWorldStatus

class VisitWorldRequestHandler(
    private val worldLoadService: WorldLoadService
) : AwareRPCHandler<VisitWorldRequest, VisitWorldResponse>
{
    override fun handle(
        request: VisitWorldRequest,
        context: AwareRPCContext<VisitWorldResponse>
    )
    {
        val response = Locks.withGlobalLock(
            "hostedworlds-load",
            request.worldGlobalId.toString()
        ) {
            val existingWorldMatchingSpec = GameManager.allHostedWorldInstances()
                .firstOrNull {
                    it.type == request.providerType &&
                        it.ownerPlayerId == request.ownerPlayerId
                }

            if (existingWorldMatchingSpec != null)
            {
                val response = HostedWorldRPC.visitWorldAddPlayerRPCService.callSync(
                    AddPlayerRequest(
                        globalWorldId = existingWorldMatchingSpec.globalId,
                        server = existingWorldMatchingSpec.server,
                        visitingPlayers = request.visitingPlayers
                    )
                )

                if (response?.status == AddPlayerStatus.SUCCESS)
                {
                    return@withGlobalLock VisitWorldResponse(
                        uniqueId = request.uniqueId,
                        status = VisitWorldStatus.SUCCESS_REDIRECT,
                        redirectToInstance = existingWorldMatchingSpec.server
                    )
                } else if (response?.status == AddPlayerStatus.FAILURE_WORLD_DRAINING)
                {
                    return@withGlobalLock VisitWorldResponse(
                        uniqueId = request.uniqueId,
                        status = VisitWorldStatus.FAILED_UNAVAILABLE_INSTANCE_DRAINING,
                        redirectToInstance = null
                    )
                }
            }

            val loadResponse = worldLoadService
                .loadWorldInstanceOnServerSync(request)
                .join()
                ?: return@withGlobalLock VisitWorldResponse(
                    uniqueId = request.uniqueId,
                    status = VisitWorldStatus.FAILED_RPC_FAILURE
                )

            if (loadResponse.status == WorldLoadStatus.LOADED)
            {
                VisitWorldResponse(
                    uniqueId = request.uniqueId,
                    status = VisitWorldStatus.SUCCESS_REDIRECT,
                    redirectToInstance = loadResponse.server
                )
            } else
            {
                VisitWorldResponse(
                    uniqueId = request.uniqueId,
                    status = VisitWorldStatus.FAILED_WORLD_GENERATION_FAILURE,
                    redirectToInstance = null
                )
            }
        }.join() ?: VisitWorldResponse(
            uniqueId = request.uniqueId,
            status = VisitWorldStatus.FAILED_NO_SERVERS_AVAILABLE,
            redirectToInstance = null
        )

        context.reply(response)
    }
}
