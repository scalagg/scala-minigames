package gg.tropic.practice.games.event

import gg.tropic.practice.games.GameImpl
import gg.scala.commons.event.StatefulEvent

class GameCompleteEvent(val game: GameImpl) : StatefulEvent()
