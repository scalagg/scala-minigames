package gg.tropic.practice.minigame

import gg.tropic.practice.profile.PracticeProfile
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player

interface MinigameLobbyCustomizer
{
    val id: String

    fun mainMenuProvider(player: Player)
    fun playProvider(player: Player)

    fun leaderboardsProvider(player: Player): Map<Int, Button>
    fun statisticsMenuProvider(profile: PracticeProfile): Map<Int, Button>

    fun holographicStatsProvider(player: Player): List<String>

    fun profileStatsNPCHeader(): List<String> = listOf(
        "${CC.B_YELLOW}CLICK FOR STATS"
    )

    fun scoreboard(): MinigameLobbyScoreboardProvider
}
