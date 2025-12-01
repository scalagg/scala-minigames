package gg.tropic.practice.games.matchmaking

import gg.tropic.practice.application.api.defaults.AwareConversationMessage
import gg.tropic.practice.games.GameReference
import java.util.UUID

/**
* @author Subham
* @since 7/20/25
*/
data class JoinIntoGameRequest(
    override val uniqueId: UUID = UUID.randomUUID(),
    val server: String,
    val players: Set<UUID>,
    val game: GameReference
) : AwareConversationMessage
