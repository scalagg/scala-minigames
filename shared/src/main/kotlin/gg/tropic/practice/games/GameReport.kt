package gg.tropic.practice.games

import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.games.player.CosmeticPlayerResources
import java.util.*

class GameReport(
    val identifier: UUID,
    val winners: List<UUID>,
    val losers: List<UUID>,
    val snapshots: Map<UUID, GameReportSnapshot>,
    val resources: Map<UUID, CosmeticPlayerResources>?,
    val duration: Long,
    val kit: String,
    val map: String,
    val status: GameReportStatus,
    val matchDate: Date = Date(),
    val extraInformation: Map<UUID, Map<String, Map<String, String>>> = mutableMapOf()
)
{
    fun usernameOf(uniqueId: UUID) = resources?.get(uniqueId)?.username ?: uniqueId.username()
}
