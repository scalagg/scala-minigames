package gg.tropic.practice.minigame.hologram

import gg.scala.game.extensions.profile.CorePlayerProfileService
import gg.scala.game.extensions.leveling.LevelSystemSyncService
import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.configuration.PracticeConfigurationService
import gg.tropic.practice.configuration.minigame.MinigameLeaderboard
import gg.tropic.practice.friendship.Friendships
import gg.tropic.practice.guilds.Guilds
import gg.tropic.practice.leaderboards.LeaderboardEntry
import gg.tropic.practice.minigame.MinigameLobby
import gg.tropic.practice.minigame.hologram.settings.LeaderboardAlignmentOption
import gg.tropic.practice.minigame.hologram.settings.LeaderboardPlayersOption
import gg.tropic.practice.minigame.hologram.settings.LeaderboardTimeOption
import gg.tropic.practice.minigame.hologram.settings.LeaderboardViewOption
import gg.tropic.practice.minigame.hologram.settings.MinigameLeaderboardSettingsMenu
import gg.tropic.practice.minigame.hologram.settings.MinigameLeaderboardSettingsService
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.services.LeaderboardManagerService
import gg.tropic.practice.statistics.StatisticRedis
import gg.tropic.practice.statistics.StatisticID
import gg.tropic.practice.statistics.TrackedKitStatistic
import gg.tropic.practice.statistics.statisticIdFrom
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.entity.hologram.HologramEntity
import net.evilblock.cubed.entity.hologram.personalized.PersonalizedHologramEntity
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class MinigameLeaderboardHologramEntity(
    val leaderboard: MinigameLeaderboard
) : PersonalizedHologramEntity(
    leaderboard.position.toLocation(Bukkit.getWorlds().first())
)
{
    companion object
    {
        private const val nameColorCacheDurationMs = 10 * 60 * 1_000L
        private const val coreWinsAggregatePrefix = "aggregate:bedwars:core:wins:"
        private const val aggregateCacheDurationMs = 8_000L
        private const val aggregateSampleLimit = 250
        private const val aroundRadius = 4

        private val sentField = PersonalizedHologramEntity::class.java
            .getDeclaredField("sent")
            .also { field ->
                field.isAccessible = true
            }

        private val coloredNameCache = ConcurrentHashMap<UUID, Pair<String, Long>>()
        private val aggregateCoreWinsCache = ConcurrentHashMap<LeaderboardTimeOption, Pair<Long, List<LeaderboardEntry>>>()
    }

    private data class PersonalLeaderboardStat(
        val score: Long,
        val rank: Long?
    )

    private val spacer = ""

    private var nextBulkLevelSyncAt = 0L

    init
    {
        persistent = false
    }

    fun configure()
    {
        initializeData()
        EntityHandler.trackEntity(this)
    }

    fun matchesRenderedEntity(playerId: UUID, entityId: Int): Boolean
    {
        val rendered = renderedFor(playerId)
            ?: return false

        return rendered
            .getMappedLines()
            .keys
            .any { hologram ->
                hologram.id == entityId
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun renderedFor(playerId: UUID): HologramEntity?
    {
        val sent = sentField.get(this) as? MutableMap<UUID, HologramEntity>
            ?: return null

        return sent[playerId]
    }

    override fun getNewLines(player: Player): List<String>
    {
        val settings = MinigameLeaderboardSettingsService
            .load(player.uniqueId, leaderboard)

        val resolved = MinigameLeaderboardSettingsService
            .resolve(leaderboard, settings)

        if (leaderboard.statisticID.equals("settings", ignoreCase = true))
        {
            return listOf(
                align(settings.alignment, "${CC.U_AQUA}Leaderboard Settings"),
                spacer,
                align(settings.alignment, "${CC.GRAY}Mode: ${CC.GREEN}${resolved.subTitle}"),
                align(settings.alignment, "${CC.GRAY}Time: ${CC.GREEN}${settings.time.displayName}"),
                align(settings.alignment, "${CC.GRAY}View: ${CC.GREEN}${settings.view.displayName}"),
                align(settings.alignment, "${CC.GRAY}Players: ${CC.GREEN}${settings.players.displayName}"),
                spacer,
                align(settings.alignment, "${CC.GOLD}Click to change settings!")
            )
        }

        populateSyntheticLevelEntry(player, resolved.statisticID)

        val lines = mutableListOf<String>()
        lines += align(settings.alignment, "${CC.U_AQUA}${resolved.title}")
        lines += align(settings.alignment, "${CC.GRAY}${resolved.subTitle}")
        lines += spacer

        val rendered = renderLeaderboardLines(player, resolved.statisticID, settings)
        if (rendered.isEmpty())
        {
            lines += align(settings.alignment, "${CC.GRAY}No leaderboard data available.")
        }
        else
        {
            lines += rendered.map { text ->
                align(settings.alignment, text)
            }
        }

        val value = resolvePersonalValue(player, resolved.statisticID)

        lines += spacer
        if (value != null && (value.score > 0L || value.rank != null))
        {
            lines += align(settings.alignment, formatPersonalEntryLine(player, value, resolved.statisticID))
        }
        else
        {
            lines += align(settings.alignment, "${CC.GRAY}You have no data yet.")
        }

        lines += spacer
        lines += align(settings.alignment, "${CC.GRAY}${resolved.resetText}")
        lines += align(settings.alignment, "${CC.GOLD}Click to change settings!")

        return lines
    }

    private fun renderLeaderboardLines(
        player: Player,
        statisticID: String,
        settings: gg.tropic.practice.minigame.hologram.settings.MinigameLeaderboardSettings
    ): List<String>
    {
        val aggregateTime = coreWinsAggregateTime(statisticID)

        return when (settings.view)
        {
            LeaderboardViewOption.Top10 -> {
                val topEntries = if (aggregateTime == null) loadTopEntries(statisticID)
                else loadAggregateCoreWinsEntries(aggregateTime)

                val filtered = filterEntries(player, topEntries, settings.players)
                    .take(10)
                    .ifEmpty {
                        if (settings.players == LeaderboardPlayersOption.All)
                        {
                            emptyList()
                        }
                        else
                        {
                            topEntries.take(10)
                        }
                    }

                formatLeaderboardEntries(
                    statisticID = statisticID,
                    entries = filtered,
                    startRank = 1
                )
            }

            LeaderboardViewOption.AroundYou -> {
                if (aggregateTime != null)
                {
                    val aggregateEntries = loadAggregateCoreWinsEntries(aggregateTime)
                    if (aggregateEntries.isEmpty())
                    {
                        return emptyList()
                    }

                    val playerIndex = aggregateEntries
                        .indexOfFirst { entry ->
                            entry.uniqueId == player.uniqueId
                        }

                    if (playerIndex < 0)
                    {
                        val filteredTop = filterEntries(player, aggregateEntries, settings.players)
                            .take(10)
                            .ifEmpty {
                                if (settings.players == LeaderboardPlayersOption.All)
                                {
                                    emptyList()
                                }
                                else
                                {
                                    aggregateEntries.take(10)
                                }
                            }

                        return formatLeaderboardEntries(
                            statisticID = statisticID,
                            entries = filteredTop,
                            startRank = 1
                        )
                    }

                    val startIndex = (playerIndex - aroundRadius)
                        .coerceAtLeast(0)
                    val endIndexExclusive = (playerIndex + aroundRadius + 1)
                        .coerceAtMost(aggregateEntries.size)

                    val aroundEntries = aggregateEntries
                        .subList(startIndex, endIndexExclusive)

                    val filtered = filterEntries(player, aroundEntries, settings.players)
                        .ifEmpty {
                            if (settings.players == LeaderboardPlayersOption.All)
                            {
                                emptyList()
                            }
                            else
                            {
                                aroundEntries
                            }
                        }

                    return formatLeaderboardEntries(
                        statisticID = statisticID,
                        entries = filtered,
                        startRank = startIndex + 1
                    )
                }

                val around = leaderboardIdsFor(statisticID)
                    .asSequence()
                    .mapNotNull { id ->
                        LeaderboardManagerService.getAroundPlayer(id, player.uniqueId)
                    }
                    .firstOrNull()

                if (around == null)
                {
                    val topEntries = loadTopEntries(statisticID)

                    val filteredTop = filterEntries(player, topEntries, settings.players)
                        .take(10)
                        .ifEmpty {
                            if (settings.players == LeaderboardPlayersOption.All)
                            {
                                emptyList()
                            }
                            else
                            {
                                topEntries.take(10)
                            }
                        }

                    return formatLeaderboardEntries(
                        statisticID = statisticID,
                        entries = filteredTop,
                        startRank = 1
                    )
                }

                val filtered = filterEntries(player, around.entries, settings.players)
                    .ifEmpty {
                        if (settings.players == LeaderboardPlayersOption.All)
                        {
                            emptyList()
                        }
                        else
                        {
                            around.entries
                        }
                    }

                formatLeaderboardEntries(
                    statisticID = statisticID,
                    entries = filtered,
                    startRank = around.startRank
                )
            }
        }
    }

    private fun formatLeaderboardEntries(
        statisticID: String,
        entries: List<LeaderboardEntry>,
        startRank: Int
    ): List<String>
    {
        val levelStatistic = isBedWarsLevelStatistic(statisticID)

        return entries.mapIndexed { index, entry ->
            formatLeaderboardLine(
                rank = startRank + index,
                uniqueId = entry.uniqueId,
                value = entry.value,
                levelStatistic = levelStatistic
            )
        }
    }

    private fun formatPersonalEntryLine(
        player: Player,
        stat: PersonalLeaderboardStat,
        statisticID: String
    ): String
    {
        val rank = stat.rank
            ?.takeIf { it > 0L }
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()

        if (rank != null)
        {
            return formatLeaderboardLine(
                rank = rank,
                uniqueId = player.uniqueId,
                value = stat.score,
                levelStatistic = isBedWarsLevelStatistic(statisticID)
            )
        }

        return "${CC.GRAY}You: ${CC.YELLOW}${Numbers.format(stat.score)}"
    }

    private fun formatLeaderboardLine(
        rank: Int,
        uniqueId: UUID,
        value: Long,
        levelStatistic: Boolean
    ): String
    {
        val guildName = Guilds.guildProvider
            .provideGuildNameFor(uniqueId)
            .join()

        val name = resolveDisplayName(uniqueId)
        val valueDisplay = formatLeaderboardValue(value, levelStatistic)

        return "${CC.YELLOW}$rank. ${CC.RESET}$name${
            if (guildName != null) " ${CC.GRAY}[$guildName${CC.GRAY}]${CC.RESET}" else ""
        } ${CC.GRAY}- $valueDisplay"
    }

    private fun formatLeaderboardValue(value: Long, levelStatistic: Boolean): String
    {
        if (!levelStatistic)
        {
            return "${CC.YELLOW}${Numbers.format(value)}"
        }

        val level = value
            .coerceAtLeast(0L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        return LevelSystemSyncService
            .byId("bedwars")
            ?.formatLevel(level)
            ?: "${CC.GRAY}[${CC.L_PURPLE}$level✫${CC.GRAY}]"
    }

    private fun resolveDisplayName(uniqueId: UUID): String
    {
        val now = System.currentTimeMillis()
        val cached = coloredNameCache[uniqueId]
        if (cached != null && now - cached.second <= nameColorCacheDurationMs)
        {
            return cached.first
        }

        val resolved = QuickAccess.coloredName(uniqueId)
            ?: runCatching {
                QuickAccess.fetchColoredName(uniqueId)
            }.getOrNull()
            ?: "${CC.GRAY}${uniqueId.username()}"

        coloredNameCache[uniqueId] = resolved to now
        return resolved
    }

    private fun isBedWarsLevelStatistic(statisticID: String): Boolean
    {
        if (!MinigameLobby.customizer().id.equals("bedwars", ignoreCase = true))
        {
            return false
        }

        return leaderboardIdsFor(statisticID)
            .any { id -> id.contains("level", ignoreCase = true) }
    }

    private fun coreWinsAggregateTime(statisticID: String): LeaderboardTimeOption?
    {
        val normalized = statisticID
            .removePrefix("custom:")

        if (!normalized.startsWith(coreWinsAggregatePrefix, ignoreCase = true))
        {
            return null
        }

        return when (normalized.substringAfter(coreWinsAggregatePrefix, "").lowercase())
        {
            LeaderboardTimeOption.Daily.key -> LeaderboardTimeOption.Daily
            LeaderboardTimeOption.Weekly.key -> LeaderboardTimeOption.Weekly
            LeaderboardTimeOption.Monthly.key -> LeaderboardTimeOption.Monthly
            LeaderboardTimeOption.Lifetime.key -> LeaderboardTimeOption.Lifetime
            else -> null
        }
    }

    private fun loadAggregateCoreWinsEntries(time: LeaderboardTimeOption): List<LeaderboardEntry>
    {
        val now = System.currentTimeMillis()
        val cached = aggregateCoreWinsCache[time]
        if (cached != null && now - cached.first <= aggregateCacheDurationMs)
        {
            return cached.second
        }

        val summed = linkedMapOf<UUID, Long>()
        coreWinsModeStatisticIds(time)
            .forEach { id ->
                redisTopEntries(id, aggregateSampleLimit)
                    .forEach { entry ->
                        summed[entry.uniqueId] = (summed[entry.uniqueId] ?: 0L) + entry.value
                    }
            }

        val aggregated = summed.entries
            .sortedByDescending { entry ->
                entry.value
            }
            .take(aggregateSampleLimit)
            .map { entry ->
                LeaderboardEntry(
                    uniqueId = entry.key,
                    value = entry.value
                )
            }

        aggregateCoreWinsCache[time] = now to aggregated
        return aggregated
    }

    private fun aggregateCoreWinsScore(playerId: UUID, time: LeaderboardTimeOption): Long
    {
        return coreWinsModeStatisticIds(time)
            .sumOf { id ->
                StatisticRedis
                    .scoreAndPositionOf(playerId, id, true)
                    ?.score
                    ?.toLong()
                    ?: 0L
            }
    }

    private fun coreWinsModeStatisticIds(time: LeaderboardTimeOption): List<String>
    {
        if (!MinigameLobby.customizer().id.equals("bedwars", ignoreCase = true))
        {
            return emptyList()
        }

        val gameModes = PracticeConfigurationService
            .minigameType()
            .provide()
            .gameModes

        return listOf("solo", "doubles", "trios", "quads")
            .mapNotNull { mode ->
                gameModes[mode]
                    ?.kitID
                    ?.takeIf { kit ->
                        kit.isNotBlank()
                    }
            }
            .distinct()
            .map { kitId ->
                winsStatisticId(kitId, time)
            }
    }

    private fun winsStatisticId(kitId: String, time: LeaderboardTimeOption): String
    {
        return statisticIdFrom(TrackedKitStatistic.Wins) {
            kit(kitId)

            when (time)
            {
                LeaderboardTimeOption.Daily -> daily()
                LeaderboardTimeOption.Weekly -> weekly()
                LeaderboardTimeOption.Monthly -> monthly()
                LeaderboardTimeOption.Lifetime -> Unit
            }
        }.toId()
    }

    private fun redisTopEntries(id: String, limit: Int): List<LeaderboardEntry>
    {
        return StatisticRedis
            .getTopPlayers(id, limit)
            .mapNotNull { scored ->
                runCatching {
                    LeaderboardEntry(
                        uniqueId = UUID.fromString(scored.value),
                        value = scored.score.toLong()
                    )
                }.getOrNull()
            }
    }

    private fun loadTopEntries(statisticID: String): List<LeaderboardEntry>
    {
        leaderboardIdsFor(statisticID).forEach { id ->
            val cached = LeaderboardManagerService.getCachedLeaderboards(id)
            if (!cached.isNullOrEmpty())
            {
                return cached
            }

            val direct = StatisticRedis
                .getTopPlayers(id, 10)
                .mapNotNull { scored ->
                    runCatching {
                        LeaderboardEntry(
                            uniqueId = UUID.fromString(scored.value),
                            value = scored.score.toLong()
                        )
                    }.getOrNull()
                }

            if (direct.isNotEmpty())
            {
                return direct
            }
        }

        return emptyList()
    }

    private fun leaderboardIdsFor(statisticID: String): List<String>
    {
        val ids = linkedSetOf<String>()
        ids += statisticID
        ids += leaderboard.statisticID

        if (statisticID.startsWith("custom:"))
        {
            ids += statisticID.removePrefix("custom:")
        }
        else
        {
            ids += "custom:$statisticID"
        }

        if (leaderboard.statisticID.startsWith("custom:"))
        {
            ids += leaderboard.statisticID.removePrefix("custom:")
        }
        else
        {
            ids += "custom:${leaderboard.statisticID}"
        }

        return ids
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun resolvePersonalValue(player: Player, statisticID: String): PersonalLeaderboardStat?
    {
        coreWinsAggregateTime(statisticID)
            ?.let { aggregateTime ->
                val aggregateScore = aggregateCoreWinsScore(player.uniqueId, aggregateTime)
                if (aggregateScore <= 0L)
                {
                    return null
                }

                val rank = loadAggregateCoreWinsEntries(aggregateTime)
                    .indexOfFirst { entry ->
                        entry.uniqueId == player.uniqueId
                    }
                    .takeIf { index ->
                        index >= 0
                    }
                    ?.plus(1)
                    ?.toLong()

                return PersonalLeaderboardStat(
                    score = aggregateScore,
                    rank = rank
                )
            }

        val ids = leaderboardIdsFor(statisticID)

        val profile = PracticeProfileService.find(player)
        if (profile != null)
        {
            ids.forEach { id ->
                val value = profile.getStatisticValue(id)
                    ?: return@forEach

                return PersonalLeaderboardStat(
                    score = value.score.toLong(),
                    rank = value.value
                        .takeIf { it >= 0L }
                        ?.plus(1L)
                )
            }
        }

        ids.forEach { id ->
            val redisKeys = linkedSetOf<String>()
            redisKeys += id
            runCatching {
                StatisticID.fromExpected(id).toRedisKey()
            }.getOrNull()
                ?.let(redisKeys::add)

            redisKeys
                .forEach { redisKey ->
                    val value = StatisticRedis.scoreAndPositionOf(
                        player.uniqueId,
                        redisKey,
                        true
                    ) ?: return@forEach

                    return PersonalLeaderboardStat(
                        score = value.score.toLong(),
                        rank = value.value + 1L
                    )
                }
        }

        return null
    }

    private fun populateSyntheticLevelEntry(player: Player, statisticID: String)
    {
        if (!MinigameLobby.customizer().id.equals("bedwars", ignoreCase = true))
        {
            return
        }

        if (!isBedWarsLevelStatistic(statisticID))
        {
            return
        }

        val shouldBulkSync = System.currentTimeMillis() >= nextBulkLevelSyncAt
        if (shouldBulkSync)
        {
            nextBulkLevelSyncAt = System.currentTimeMillis() + 30_000L
        }

        val targets = if (shouldBulkSync)
        {
            Bukkit.getOnlinePlayers().toList()
        }
        else
        {
            listOf(player)
        }

        val targetLevels = targets
            .associateWith { target ->
                runCatching {
                    CorePlayerProfileService
                        .find(target)
                        ?.getGameLevelData("bedwars")
                        ?.level
                        ?.toLong()
                }.getOrNull()
            }
            .filterValues { value ->
                value != null
            }

        if (targetLevels.isEmpty())
        {
            return
        }

        leaderboardIdsFor(statisticID).forEach { id ->
            val normalized = id.removePrefix("custom:")

            listOf(
                normalized,
                "custom:$normalized"
            ).forEach { leaderboardId ->
                val redisKey = runCatching {
                    StatisticID.fromExpected(leaderboardId).toRedisKey()
                }.getOrElse {
                    leaderboardId
                }

                targetLevels.forEach { (target, targetLevel) ->
                    StatisticRedis.save(
                        target.uniqueId,
                        redisKey,
                        targetLevel!!
                    )
                }
            }
        }
    }

    private fun filterEntries(
        player: Player,
        entries: List<LeaderboardEntry>,
        playersOption: LeaderboardPlayersOption
    ): List<LeaderboardEntry>
    {
        return when (playersOption)
        {
            LeaderboardPlayersOption.All -> entries

            LeaderboardPlayersOption.Friends,
            LeaderboardPlayersOption.BestFriends -> entries.filter { entry ->
                entry.uniqueId == player.uniqueId || Friendships
                    .requirements
                    .existsBetween(player.uniqueId, entry.uniqueId)
                    .join()
            }

            LeaderboardPlayersOption.GuildMembers -> {
                val ownGuild = Guilds.guildProvider
                    .provideGuildNameFor(player.uniqueId)
                    .join()
                if (ownGuild == null)
                {
                    return emptyList()
                }

                entries.filter { entry ->
                    val memberGuild = Guilds.guildProvider
                        .provideGuildNameFor(entry.uniqueId)
                        .join()

                    memberGuild != null && memberGuild.equals(ownGuild, ignoreCase = true)
                }
            }
        }
    }

    private fun align(alignment: LeaderboardAlignmentOption, text: String): String
    {
        if (alignment != LeaderboardAlignmentOption.Center || text.isBlank())
        {
            return text
        }

        val stripped = ChatColor.stripColor(text)
            ?: text

        val width = 32
        val spaces = ((width - stripped.length) / 2)
            .coerceAtLeast(0)

        return " ".repeat(spaces) + text
    }

    override fun onRightClick(player: Player)
    {
        MinigameLeaderboardSettingsMenu(
            leaderboard = leaderboard,
            settings = MinigameLeaderboardSettingsService.load(player.uniqueId, leaderboard)
        ).openMenu(player)
    }

    override fun onLeftClick(player: Player)
    {
        onRightClick(player)
    }

    override fun getUpdateInterval() = 3_000L
}
