package gg.tropic.practice.minigame.event

import gg.scala.commons.event.StatelessEvent
import gg.tropic.practice.minigame.MiniGameLifecycle
import gg.tropic.practice.minigame.rejoin.RejoinToken
import gg.tropic.practice.minigame.rejoin.TrackedPlayerRejoin
import java.util.UUID

data class PlayerMiniGameRejoinTokenExpiredEvent(
    val game: MiniGameLifecycle<*>,
    val player: UUID,
    val token: TrackedPlayerRejoin,
) : StatelessEvent()
