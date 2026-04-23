package gg.tropic.practice.ugc.generation.addplayer

import gg.tropic.practice.application.api.defaults.AwareConversationMessage
import java.util.UUID

data class AddPlayerRequest(
    override val uniqueId: UUID = UUID.randomUUID(),
    val globalWorldId: UUID,
    val server: String,
    val visitingPlayers: Set<UUID>
) : AwareConversationMessage
