package gg.tropic.practice.ugc.generation

import gg.tropic.practice.application.api.defaults.AwareConversationMessage
import gg.tropic.practice.ugc.generation.visits.VisitWorldRequest
import java.util.UUID

data class WorldLoadRequest(
    override val uniqueId: UUID = UUID.randomUUID(),
    val visitWorldRequest: VisitWorldRequest,
    val server: String
) : AwareConversationMessage
