package gg.tropic.practice.ugc.generation.visits

import gg.tropic.practice.application.api.defaults.AwareConversationMessage
import gg.tropic.practice.ugc.WorldInstanceProviderType
import java.util.UUID

data class VisitWorldRequest(
    override val uniqueId: UUID = UUID.randomUUID(),
    val ownerPlayerId: UUID,
    val visitingPlayers: Set<UUID>,
    val worldGlobalId: UUID,
    val configuration: VisitWorldRequestConfiguration,
    val providerType: WorldInstanceProviderType,
) : AwareConversationMessage
