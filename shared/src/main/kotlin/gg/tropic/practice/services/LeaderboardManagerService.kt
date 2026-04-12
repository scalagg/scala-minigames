package gg.tropic.practice.services

import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.util.QuickAccess
import gg.tropic.practice.statistics.StatisticRedis
import gg.tropic.practice.guilds.Guilds
import gg.tropic.practice.leaderboards.LeaderboardEntry
import gg.tropic.practice.leaderboards.StatisticLeaderboard
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.statistics.StatisticService
import mc.arch.minigames.parties.toDisplayName
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
object LeaderboardManagerService
{
    data class LeaderboardWindow(
        val startRank: Int,
        val entries: List<LeaderboardEntry>
    )

    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private var top10LeaderboardCache = mutableMapOf<String, List<LeaderboardEntry>>()
    private var top10FormattedLeaderboardCache = mutableMapOf<String, List<String>>()

    fun getCachedFormattedLeaderboards(leaderboardID: String) = top10FormattedLeaderboardCache[leaderboardID]
        ?: listOf("${CC.GRAY}No one is on the top 10!")

    fun getCachedLeaderboards(leaderboardID: String) = top10LeaderboardCache[leaderboardID]

    fun formatMenuLeaderboard(player: Player, statisticID: StatisticLeaderboard): List<String>
    {
        val statisticID = statisticID
            .commonlyViewedLeaderboardType
            .toStatisticID(statisticID.kit)

        val value = PracticeProfileService.find(player)
            ?.getStatisticValue(statisticID)
            ?: return getCachedFormattedLeaderboards(statisticID.toId())

        val personalScore = listOf(
            "${CC.PRI}Your score: ${CC.WHITE}${
                Numbers.format(value.score.toLong())
            } ${
                if (value.value != -1L) "${CC.GRAY}(#${
                    Numbers.format(value.value + 1)
                })" else ""
            }"
        )

        return personalScore + getCachedFormattedLeaderboards(statisticID.toId())
    }

    fun mapToFancy(entries: List<LeaderboardEntry>, startRank: Int = 1) = entries.mapIndexed { index, entry ->
        val guildName = Guilds.guildProvider
            .provideGuildNameFor(entry.uniqueId)
            .join()

        "${CC.PRI}#${startRank + index}. ${CC.WHITE}${
            entry.uniqueId.toDisplayName()
        }${
            if (guildName != null) " ${CC.GRAY}[$guildName${CC.GRAY}]${CC.RESET}" else ""
        } ${CC.GRAY}- ${CC.WHITE}${
            Numbers.format(entry.value)
        }"
    }

    fun getAroundPlayer(leaderboardID: String, player: UUID, radius: Int = 4): LeaderboardWindow?
    {
        val rank = StatisticRedis
            .scoreAndPositionOf(player, leaderboardID, true)
            ?.value
            ?.toInt()
            ?: return null

        val start = (rank - radius)
            .coerceAtLeast(0)

        val end = rank + radius

        val entries = StatisticRedis
            .getPlayersByRankRange(leaderboardID, start, end)
            .map { scoredValue ->
                LeaderboardEntry(
                    uniqueId = UUID.fromString(scoredValue.value),
                    value = scoredValue.score.toLong()
                )
            }

        return LeaderboardWindow(
            startRank = start + 1,
            entries = entries
        )
    }

    fun rebuildLeaderboardCaches()
    {
        val newLeaderboardCache = mutableMapOf<String, List<LeaderboardEntry>>()
        val newFormattedLeaderboardCache = mutableMapOf<String, List<String>>()
        StatisticService.trackedStatistics().forEach { statistic ->
            val topTenPlayers = statistic.asyncLoadTopTenPlayers()
            newLeaderboardCache[statistic.id.toId()] = topTenPlayers
            newFormattedLeaderboardCache[statistic.id.toId()] = mapToFancy(topTenPlayers)
        }

        this.top10FormattedLeaderboardCache = newFormattedLeaderboardCache
        this.top10LeaderboardCache = newLeaderboardCache
    }

    @Configure
    fun configure()
    {
        Schedulers
            .async()
            .runRepeating({ _ ->
                rebuildLeaderboardCaches()
            }, 0L, TimeUnit.SECONDS, 10L, TimeUnit.SECONDS)
    }
}
