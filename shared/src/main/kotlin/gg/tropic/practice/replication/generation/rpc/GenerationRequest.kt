package gg.tropic.practice.replication.generation.rpc

import gg.tropic.practice.application.api.defaults.AwareConversationMessage
import gg.tropic.practice.expectation.GameExpectation
import java.util.UUID

data class GenerationRequest(
    override val uniqueId: UUID = UUID.randomUUID(),
    val server: String,
    val requirement: GenerationRequirement,
    val map: String,
    val expectation: GameExpectation
) : AwareConversationMessage
