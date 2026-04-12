package gg.tropic.practice.games.duels

import gg.tropic.practice.region.Region
import java.util.UUID

data class DuelRequest(
    val requester: UUID,
    val requesterPing: Int,
    val requestee: UUID,
    val region: Region,
    val kitID: String,
    val mapID: String? = null,
    val configuration: PvPConfiguration? = null
)

