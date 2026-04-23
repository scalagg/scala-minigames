package gg.tropic.practice.games.matchmaking

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import java.util.UUID

data class JoinIntoGameResult(
    override val uniqueId: UUID,
    val status: JoinIntoGameStatus
) : AwareConversationMessageResponse
