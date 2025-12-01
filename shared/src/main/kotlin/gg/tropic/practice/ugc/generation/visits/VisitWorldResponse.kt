package gg.tropic.practice.ugc.generation.visits

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import java.util.UUID

/**
 * @author Subham
 * @since 7/20/25
 */
data class VisitWorldResponse(
    override val uniqueId: UUID,
    val status: VisitWorldStatus,
    val redirectToInstance: String? = null
) : AwareConversationMessageResponse
