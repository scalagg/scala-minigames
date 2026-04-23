package gg.tropic.practice.games.bots

import java.util.UUID

data class BotGameMetadata(
    val botNamespace: String,
    val botProfileID: String?,
    val kitID: String,
    val mapID: String? = null,

    val botInstances: Set<UUID>
)
