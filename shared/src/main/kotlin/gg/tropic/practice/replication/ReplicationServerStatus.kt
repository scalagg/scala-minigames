package gg.tropic.practice.replication

import gg.tropic.practice.application.api.defaults.AwareConversationMessage
import java.util.UUID

data class ReplicationServerStatus(
    override val uniqueId: UUID = UUID.randomUUID(),
    val server: String,
    val status: ServerAvailableReplicationState
) : AwareConversationMessage
