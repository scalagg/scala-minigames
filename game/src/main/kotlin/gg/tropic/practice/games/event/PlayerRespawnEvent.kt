package gg.tropic.practice.games.event

import gg.scala.commons.event.StatelessEvent
import gg.tropic.practice.games.GameImpl
import org.bukkit.entity.Player

data class PlayerRespawnEvent(
    val game: GameImpl,
    val player: Player
) : StatelessEvent()
