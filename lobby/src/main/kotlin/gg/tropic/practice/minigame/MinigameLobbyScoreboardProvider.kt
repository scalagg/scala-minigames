package gg.tropic.practice.minigame

import gg.tropic.practice.player.LobbyPlayer
import org.bukkit.entity.Player

interface MinigameLobbyScoreboardProvider
{
    fun provideTitle(): String
    fun provideIdleLines(player: Player, lobbyProfile: LobbyPlayer): List<String>
}
