package gg.tropic.practice.expectation

import gg.tropic.practice.games.duels.PvPConfiguration
import gg.tropic.practice.games.matchmaking.MatchmakingMetadata
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.minigame.MiniGameConfiguration
import gg.tropic.practice.provider.MiniProviderVersion
import java.util.*

data class GameExpectation(
    val identifier: UUID,
    val players: MutableSet<UUID>,
    val teams: Set<GameTeam>,
    val kitId: String,
    val mapId: String,

    val queueType: QueueType? = null,
    val queueId: String? = null,
    val configuration: PvPConfiguration? = null,
    val matchmakingMetadataAPIV2: MatchmakingMetadata? = null,
    val miniGameConfiguration: MiniGameConfiguration? = null,
    val requiredMiniVersion: MiniProviderVersion = MiniProviderVersion.LEGACY
)
