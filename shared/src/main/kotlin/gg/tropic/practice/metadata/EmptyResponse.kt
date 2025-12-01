package gg.tropic.practice.metadata

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import java.util.UUID

/**
 * Empty response wrapper for RPC calls that don't need to return data.
 * Used when the response type would otherwise be Unit.
 */
data class EmptyResponse(
    override val uniqueId: UUID
) : AwareConversationMessageResponse

