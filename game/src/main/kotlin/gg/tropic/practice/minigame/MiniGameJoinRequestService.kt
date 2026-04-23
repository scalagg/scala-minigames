package gg.tropic.practice.minigame

import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.minigame.handler.MiniGameJoinIntoGameHandler
import gg.tropic.practice.minigame.handler.MiniGameSpectateHandler

@Service
object MiniGameJoinRequestService
{
    @Configure
    fun configure()
    {
        MiniGameRPC.spectateService.addHandler(MiniGameSpectateHandler())
        MiniGameRPC.joinIntoGameService.addHandler(MiniGameJoinIntoGameHandler())
    }
}
