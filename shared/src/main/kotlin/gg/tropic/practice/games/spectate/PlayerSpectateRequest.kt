package gg.tropic.practice.games.spectate

import java.util.*

data class PlayerSpectateRequest(
    val player: UUID,
    val target: UUID,
    val bypassesSpectatorAllowanceChecks: Boolean = false
)
