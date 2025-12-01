package gg.tropic.practice.ugc.handler

import gg.scala.commons.agnostic.sync.ServerSync
import gg.tropic.practice.application.api.defaults.AwareRPCContext
import gg.tropic.practice.application.api.defaults.AwareRPCHandler
import gg.tropic.practice.ugc.HostedWorldInstanceService
import gg.tropic.practice.ugc.HostedWorldState
import gg.tropic.practice.ugc.generation.addplayer.AddPlayerRequest
import gg.tropic.practice.ugc.generation.addplayer.AddPlayerResponse
import gg.tropic.practice.ugc.generation.addplayer.AddPlayerStatus

/**
 * @author Subham
 * @since 7/24/25
 */
class AddPlayerHandler : AwareRPCHandler<AddPlayerRequest, AddPlayerResponse>
{
    override fun handle(
        request: AddPlayerRequest,
        context: AwareRPCContext<AddPlayerResponse>
    )
    {
        if (request.server != ServerSync.local.id)
        {
            return
        }

        val hostedWorld = HostedWorldInstanceService
            .instanceByGID(request.globalWorldId)
            ?: return run {
                context.reply(AddPlayerResponse(
                    uniqueId = request.uniqueId,
                    status = AddPlayerStatus.FAILURE_WORLD_NOT_LOADED
                ))
            }

        if (hostedWorld.state == HostedWorldState.DRAINING)
        {
            context.reply(AddPlayerResponse(
                uniqueId = request.uniqueId,
                status = AddPlayerStatus.FAILURE_WORLD_DRAINING
            ))
            return
        }

        request.visitingPlayers.forEach { player ->
            HostedWorldInstanceService.trackPendingLogin(player, hostedWorld)
        }

        context.reply(AddPlayerResponse(
            uniqueId = request.uniqueId,
            status = AddPlayerStatus.SUCCESS
        ))
    }
}
