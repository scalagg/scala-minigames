package gg.tropic.practice.minigame.event

import gg.scala.commons.event.StatelessEvent
import gg.tropic.practice.minigame.AbstractMiniGameGameImpl
import org.bukkit.entity.Player

data class PlayerMiniGameQuitWhileStartingEvent(
    val game: AbstractMiniGameGameImpl<*>,
    val player: Player
) : StatelessEvent()
