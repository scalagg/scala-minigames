package gg.tropic.practice.metadata

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import java.util.UUID

data class EmptyResponse(
    override val uniqueId: UUID
) : AwareConversationMessageResponse

