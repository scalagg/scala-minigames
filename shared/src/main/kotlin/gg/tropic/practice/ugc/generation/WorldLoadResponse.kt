package gg.tropic.practice.ugc.generation

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import java.util.UUID

/**
 * @author Subham
 * @since 7/18/25
 */
data class WorldLoadResponse(
    override val uniqueId: UUID,
    val status: WorldLoadStatus,
    val server: String?
) : AwareConversationMessageResponse
