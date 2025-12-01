package gg.tropic.practice.games.spectate

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import java.util.UUID

/**
 * @author Subham
 * @since 7/20/25
 */
data class SpectateResponse(
    override val uniqueId: UUID,
    val status: SpectateResponseStatus
) : AwareConversationMessageResponse
