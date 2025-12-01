package gg.tropic.practice.metadata

import gg.tropic.practice.application.api.defaults.AwareConversationMessage
import java.util.UUID

/**
 * @author Subham
 * @since 7/20/25
 */
data class ServerInstanceMetadata(
    override val uniqueId: UUID = UUID.randomUUID(),
    val server: String,
    val metadata: InstanceMetadata
) : AwareConversationMessage
