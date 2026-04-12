package gg.tropic.practice.menu.party

import org.bukkit.entity.Player
import java.util.UUID

object PartyPlayBotsIntegration
{
    var createBotsWith: (Player, Set<UUID>) -> Unit = { _, _ -> }
}
