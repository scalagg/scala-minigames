package gg.tropic.practice.minigame.event

import gg.scala.commons.event.StatelessEvent
import gg.tropic.practice.minigame.AbstractMiniGameGameImpl

data class MiniGameEnterStartEvent(
    val game: AbstractMiniGameGameImpl<*>
) : StatelessEvent()
