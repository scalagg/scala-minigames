package gg.tropic.practice.games.player

import java.util.UUID

data class CosmeticPlayerResources(
    val playerId: UUID,
    val username: String,
    val displayName: String,
    val disguised: Boolean
)
