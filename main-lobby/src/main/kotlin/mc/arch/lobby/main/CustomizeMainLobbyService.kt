package mc.arch.lobby.main

import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.minigame.MinigameLobby

@Service
object CustomizeMainLobbyService
{
    @Configure
    fun configure()
    {
        MinigameLobby.customizeMainLobby()
    }
}
