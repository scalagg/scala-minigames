package gg.tropic.practice.games.matchmaking

import gg.tropic.practice.region.Region

data class MatchmakingMetadata(
    val region: Region,
    val bracket: String?
)
