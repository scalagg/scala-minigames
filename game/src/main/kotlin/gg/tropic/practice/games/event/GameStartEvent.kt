package gg.tropic.practice.games.event

import gg.tropic.practice.games.GameImpl
import gg.scala.commons.event.StatefulEvent

class GameStartEvent(
    val game: GameImpl,
    var cancelMessage: String = "Failed to start game"
) : StatefulEvent()
