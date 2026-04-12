package gg.tropic.practice.ugc.generation.addplayer

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import java.util.UUID

data class AddPlayerResponse(
    override val uniqueId: UUID,
    val status: AddPlayerStatus
) : AwareConversationMessageResponse
