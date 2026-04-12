package gg.tropic.practice.games.player

import org.bukkit.entity.Player

object SpectatingPlayerProvider
{
    var spectating: (Player) -> Boolean = { it.hasMetadata("spectator") }
}
