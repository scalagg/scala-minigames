package gg.tropic.practice.replication.generation.rpc

import gg.tropic.practice.application.api.defaults.AwareConversationMessageResponse
import gg.tropic.practice.replication.ReplicationResultStatus
import java.util.UUID

data class GenerationResult(
    override val uniqueId: UUID = UUID.randomUUID(),
    val status: ReplicationResultStatus,
    val message: String? = null
) : AwareConversationMessageResponse
