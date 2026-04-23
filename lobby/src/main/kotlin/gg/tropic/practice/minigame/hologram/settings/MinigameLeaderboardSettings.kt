package gg.tropic.practice.minigame.hologram.settings

import gg.tropic.practice.commands.networkTransaction
import gg.tropic.practice.configuration.minigame.MinigameLeaderboard
import gg.tropic.practice.configuration.PracticeConfigurationService
import gg.tropic.practice.minigame.MinigameLobby
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.statistics.StatisticID
import gg.tropic.practice.statistics.StatisticLifetime
import gg.tropic.practice.statistics.TrackedKitStatistic
import gg.tropic.practice.statistics.statisticIdFrom
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * @author Limo
 * @date 2/22/2026
 */
enum class LeaderboardTimeOption(
    val key: String,
    val displayName: String,
    val lifetime: StatisticLifetime?
)
{
    Daily("daily", "Daily", StatisticLifetime.Daily),
    Weekly("weekly", "Weekly", StatisticLifetime.Weekly),
    Monthly("monthly", "Monthly", StatisticLifetime.Monthly),
    Lifetime("lifetime", "Lifetime", null);

    companion object
    {
        fun fromKey(key: String?) = entries
            .firstOrNull { it.key.equals(key, ignoreCase = true) }
            ?: Weekly
    }
}

/**
 * @author Limo
 * @date 2/22/2026
 */
enum class LeaderboardViewOption(
    val key: String,
    val displayName: String
)
{
    Top10("top10", "Top 10"),
    AroundYou("around", "Players Around You");

    companion object
    {
        fun fromKey(key: String?) = entries
            .firstOrNull { it.key.equals(key, ignoreCase = true) }
            ?: Top10
    }
}

/**
 * @author Limo
 * @date 2/22/2026
 */
enum class LeaderboardPlayersOption(
    val key: String,
    val displayName: String
)
{
    All("all", "All"),
    Friends("friends", "Friends"),
    BestFriends("best_friends", "Best Friends"),
    GuildMembers("guild_members", "Guild Members");

    companion object
    {
        fun fromKey(key: String?) = entries
            .firstOrNull { it.key.equals(key, ignoreCase = true) }
            ?: All
    }
}

/**
 * @author Limo
 * @date 2/22/2026
 */
enum class LeaderboardAlignmentOption(
    val key: String,
    val displayName: String
)
{
    Center("center", "Center"),
    Block("block", "Block");

    companion object
    {
        fun fromKey(key: String?) = entries
            .firstOrNull { it.key.equals(key, ignoreCase = true) }
            ?: Block
    }
}

/**
 * @author Limo
 * @date 2/22/2026
 */
data class LeaderboardModeOption(
    val key: String,
    val displayName: String,
    val statModeKey: String? = null
)

/**
 * @author Limo
 * @date 2/22/2026
 */
data class MinigameLeaderboardSettings(
    var mode: String,
    var time: LeaderboardTimeOption,
    var view: LeaderboardViewOption,
    var players: LeaderboardPlayersOption,
    var alignment: LeaderboardAlignmentOption
)

/**
 * @author Limo
 * @date 2/22/2026
 */
data class ResolvedMinigameLeaderboard(
    val title: String,
    val subTitle: String,
    val statisticID: String,
    val resetText: String,
    val modeOptions: List<LeaderboardModeOption>,
    val timeOptions: List<LeaderboardTimeOption>
)

/**
 * @author Limo
 * @date 2/22/2026
 */
object MinigameLeaderboardSettingsService
{
    private enum class BedWarsLeaderboardMetric
    {
        Level,
        Wins,
        FinalKills,
        BedsBroken,
        Unknown
    }

    private val defaultModes = listOf(
        LeaderboardModeOption(
            key = "all",
            displayName = "All Modes"
        )
    )

    private val bedWarsModes = listOf(
        LeaderboardModeOption("core", "Core Modes"),
        LeaderboardModeOption("solo", "Solo", "solo"),
        LeaderboardModeOption("doubles", "Doubles", "doubles"),
        LeaderboardModeOption("trios", "3v3v3v3", "trios"),
        LeaderboardModeOption("quads", "4v4v4v4", "quads"),
        LeaderboardModeOption("fours", "4v4", "fours")
    )

    private val bedWarsLevelMode = listOf(
        LeaderboardModeOption("all", "All Modes")
    )

    private val allTimes = listOf(
        LeaderboardTimeOption.Daily,
        LeaderboardTimeOption.Weekly,
        LeaderboardTimeOption.Monthly,
        LeaderboardTimeOption.Lifetime
    )

    private val lifetimeOnly = listOf(
        LeaderboardTimeOption.Lifetime
    )

    fun load(playerId: UUID, leaderboard: MinigameLeaderboard): MinigameLeaderboardSettings
    {
        val defaults = defaultsFor(leaderboard)
        val profile = PracticeProfileService.find(playerId)
            ?: return defaults

        val key = keyFor(leaderboard)
        val mode = profile.leaderboardPreference("$key:mode", defaults.mode)
        val time = LeaderboardTimeOption.fromKey(
            profile.leaderboardPreference("$key:time", defaults.time.key)
        )
        val view = LeaderboardViewOption.fromKey(
            profile.leaderboardPreference("$key:view", defaults.view.key)
        )
        val players = LeaderboardPlayersOption.fromKey(
            profile.leaderboardPreference("$key:players", defaults.players.key)
        )
        val alignment = LeaderboardAlignmentOption.fromKey(
            profile.leaderboardPreference("$key:alignment", defaults.alignment.key)
        )

        return MinigameLeaderboardSettings(
            mode = mode,
            time = time,
            view = view,
            players = players,
            alignment = alignment
        )
    }

    fun save(
        playerId: UUID,
        leaderboard: MinigameLeaderboard,
        settings: MinigameLeaderboardSettings
    ): CompletableFuture<Void>
    {
        val key = keyFor(leaderboard)
        return playerId.networkTransaction {
            setLeaderboardPreference("$key:mode", settings.mode)
            setLeaderboardPreference("$key:time", settings.time.key)
            setLeaderboardPreference("$key:view", settings.view.key)
            setLeaderboardPreference("$key:players", settings.players.key)
            setLeaderboardPreference("$key:alignment", settings.alignment.key)
        }
    }

    fun resolve(leaderboard: MinigameLeaderboard, input: MinigameLeaderboardSettings): ResolvedMinigameLeaderboard
    {
        val metric = detectMetric(leaderboard)
        val modeOptions = modeOptionsFor(metric)
        val timeOptions = timeOptionsFor(metric)

        val selectedMode = modeOptions.firstOrNull {
            it.key.equals(input.mode, ignoreCase = true)
        } ?: modeOptions.first()

        val selectedTime = timeOptions.firstOrNull {
            it.key.equals(input.time.key, ignoreCase = true)
        } ?: timeOptions.first()

        val statisticId = when
        {
            MinigameLobby.customizer().id.equals("bedwars", ignoreCase = true) ->
            {
                resolveBedWarsStatisticId(
                    leaderboard = leaderboard,
                    metric = metric,
                    mode = selectedMode,
                    time = selectedTime
                )
            }

            else -> leaderboard.statisticID
        }

        val title = when (metric)
        {
            BedWarsLeaderboardMetric.Wins -> titleFor("Wins", selectedTime)
            BedWarsLeaderboardMetric.FinalKills -> titleFor("Final Kills", selectedTime)
            BedWarsLeaderboardMetric.BedsBroken -> titleFor("Beds Broken", selectedTime)
            BedWarsLeaderboardMetric.Level -> "Bed Wars Level"
            BedWarsLeaderboardMetric.Unknown -> leaderboard.displayName
        }

        val subtitle = selectedMode.displayName
        val resetText = if (selectedTime.lifetime == null)
        {
            "Never resets."
        }
        else
        {
            val days = max(1, TimeUnit.MILLISECONDS.toDays(selectedTime.lifetime.getTimeUntilReset()).toInt())
            "Resets in $days day${if (days == 1) "" else "s"}."
        }

        return ResolvedMinigameLeaderboard(
            title = title,
            subTitle = subtitle,
            statisticID = statisticId,
            resetText = resetText,
            modeOptions = modeOptions,
            timeOptions = timeOptions
        )
    }

    fun defaultsFor(leaderboard: MinigameLeaderboard): MinigameLeaderboardSettings
    {
        val source = "${leaderboard.displayName} ${leaderboard.statisticID}".lowercase()
        val metric = detectMetric(leaderboard)
        val modeDefaults = modeOptionsFor(metric)
        val timeDefaults = timeOptionsFor(metric)

        val mode = when
        {
            source.contains("solo") -> "solo"
            source.contains("double") -> "doubles"
            source.contains("3v3") || source.contains("trios") -> "trios"
            source.contains("4v4v4v4") || source.contains("quads") -> "quads"
            source.contains("4v4") -> "fours"
            source.contains("all modes") -> modeDefaults.first().key
            else -> modeDefaults.first().key
        }

        val time = when
        {
            source.contains("daily") -> LeaderboardTimeOption.Daily
            source.contains("weekly") -> LeaderboardTimeOption.Weekly
            source.contains("monthly") -> LeaderboardTimeOption.Monthly
            else -> LeaderboardTimeOption.Lifetime
        }

        return MinigameLeaderboardSettings(
            mode = modeDefaults.firstOrNull { it.key == mode }?.key ?: modeDefaults.first().key,
            time = timeDefaults.firstOrNull { it == time } ?: timeDefaults.first(),
            view = LeaderboardViewOption.Top10,
            players = LeaderboardPlayersOption.All,
            alignment = LeaderboardAlignmentOption.Block
        )
    }

    private fun resolveBedWarsStatisticId(
        leaderboard: MinigameLeaderboard,
        metric: BedWarsLeaderboardMetric,
        mode: LeaderboardModeOption,
        time: LeaderboardTimeOption
    ): String
    {
        if (metric == BedWarsLeaderboardMetric.Level)
        {
            return leaderboard.statisticID
        }

        if (metric == BedWarsLeaderboardMetric.Wins)
        {
            if (mode.key.equals("core", ignoreCase = true))
            {
                return "aggregate:bedwars:core:wins:${time.key}"
            }

            val kitId = mode.statModeKey
                ?.let(::bedWarsModeKitId)
                ?: bedWarsModeKitId(mode.key)

            val id = statisticIdFrom(TrackedKitStatistic.Wins) {
                kit(kitId)

                when (time)
                {
                    LeaderboardTimeOption.Daily -> daily()
                    LeaderboardTimeOption.Weekly -> weekly()
                    LeaderboardTimeOption.Monthly -> monthly()
                    LeaderboardTimeOption.Lifetime -> Unit
                }
            }

            return id.toId()
        }

        val customKey = when (metric)
        {
            BedWarsLeaderboardMetric.FinalKills -> "final-kills"
            BedWarsLeaderboardMetric.BedsBroken -> "beds-broken"
            else -> return leaderboard.statisticID
        }

        val base = mode.statModeKey
            ?.let { "bedwars:mode:$it:$customKey" }
            ?: "bedwars:core:$customKey"

        val customId = if (time == LeaderboardTimeOption.Lifetime)
        {
            StatisticID.fromCustom(base)
        }
        else
        {
            StatisticID.fromCustom(
                id = "$base:${time.key}",
                lifetime = time.lifetime
            )
        }

        return customId.toId()
    }

    private fun bedWarsModeKitId(mode: String): String
    {
        val modes = PracticeConfigurationService
            .minigameType()
            .provide()
            .gameModes

        return when (mode)
        {
            "solo" -> modes["solo"]?.kitID
            "doubles" -> modes["doubles"]?.kitID
            "trios" -> modes["trios"]?.kitID
            "quads" -> modes["quads"]?.kitID
            "fours" -> modes["4v4"]?.kitID
            else -> null
        } ?: "bedwars"
    }

    private fun modeOptionsFor(metric: BedWarsLeaderboardMetric): List<LeaderboardModeOption>
    {
        if (MinigameLobby.customizer().id.equals("bedwars", ignoreCase = true))
        {
            return if (metric == BedWarsLeaderboardMetric.Level) bedWarsLevelMode
            else bedWarsModes
        }

        return defaultModes
    }

    private fun timeOptionsFor(metric: BedWarsLeaderboardMetric): List<LeaderboardTimeOption>
    {
        if (
            MinigameLobby.customizer().id.equals("bedwars", ignoreCase = true) &&
            metric == BedWarsLeaderboardMetric.Level
        )
        {
            return lifetimeOnly
        }

        return allTimes
    }

    private fun titleFor(base: String, time: LeaderboardTimeOption): String
    {
        return "${time.displayName} $base"
    }

    private fun detectMetric(leaderboard: MinigameLeaderboard): BedWarsLeaderboardMetric
    {
        val source = "${leaderboard.displayName} ${leaderboard.statisticID}".lowercase()

        return when
        {
            source.contains("level") -> BedWarsLeaderboardMetric.Level
            source.contains("final-kills") || source.contains("final kills") -> BedWarsLeaderboardMetric.FinalKills
            source.contains("beds-broken") || source.contains("beds broken") -> BedWarsLeaderboardMetric.BedsBroken
            source.contains("wins") -> BedWarsLeaderboardMetric.Wins
            else -> BedWarsLeaderboardMetric.Unknown
        }
    }

    private fun keyFor(leaderboard: MinigameLeaderboard): String
    {
        return "minigame-lb:${MinigameLobby.customizer().id.lowercase()}"
    }
}
