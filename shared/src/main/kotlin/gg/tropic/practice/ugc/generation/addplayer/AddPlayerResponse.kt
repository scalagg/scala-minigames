package gg.tropic.practice.ugc.generation.addplayer

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import java.util.UUID

/**
 * @author Subham
 * @since 7/24/25
 */
data class AddPlayerResponse(
    override val uniqueId: UUID,
    val status: AddPlayerStatus
) : AwareConversationMessageResponse
