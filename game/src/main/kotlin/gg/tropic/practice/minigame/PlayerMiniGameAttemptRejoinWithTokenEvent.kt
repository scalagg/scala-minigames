package gg.tropic.practice.minigame

import gg.scala.commons.event.StatefulEvent
import gg.scala.commons.event.StatelessEvent
import gg.tropic.practice.games.team.TeamIdentifier
import gg.tropic.practice.minigame.AbstractMiniGameGameImpl
import gg.tropic.practice.minigame.MiniGameLifecycle
import org.bukkit.entity.Player
import java.util.UUID

data class PlayerMiniGameAttemptRejoinWithTokenEvent(
    val game: MiniGameLifecycle<*>,
    val player: UUID,
    val previousTeamIdentifier: TeamIdentifier,
    var shouldAllowLogin: Boolean = true,
    var persistentSpectator: Boolean = false
) : StatefulEvent()
