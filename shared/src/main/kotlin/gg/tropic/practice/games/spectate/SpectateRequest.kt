package gg.tropic.practice.games.spectate

import gg.tropic.practice.application.api.defaults.AwareConversationMessage
import gg.tropic.practice.games.GameReference
import java.util.UUID

data class SpectateRequest(
    override val uniqueId: UUID = UUID.randomUUID(),
    val server: String,
    val gameId: UUID,
    val player: UUID,
    val target: UUID,
    val bypassesSpectatorAllowanceChecks: Boolean = false
) : AwareConversationMessage
