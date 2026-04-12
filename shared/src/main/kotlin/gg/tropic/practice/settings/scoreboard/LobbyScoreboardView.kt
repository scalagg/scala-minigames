package gg.tropic.practice.settings.scoreboard

import gg.scala.basics.plugin.settings.SettingValue
import org.bukkit.entity.Player

enum class LobbyScoreboardView(private val display: String) : SettingValue
{
    Dev("System"), Staff("Staff"), None("");

    override val displayName: String
        get() = display

    override fun display(player: Player) = if (this == None)
    {
        true
    } else
    {
        player
            .hasPermission(
                "practice.lobby.scoreboard.views.$name"
            )
    }
}
