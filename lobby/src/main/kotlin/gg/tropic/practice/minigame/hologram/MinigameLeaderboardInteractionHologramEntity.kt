package gg.tropic.practice.minigame.hologram

import gg.tropic.practice.configuration.minigame.MinigameLeaderboard
import gg.tropic.practice.minigame.hologram.settings.MinigameLeaderboardSettingsMenu
import gg.tropic.practice.minigame.hologram.settings.MinigameLeaderboardSettingsService
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.entity.hologram.HologramEntity
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author Limo
 * @date 2/22/2026
 */
class MinigameLeaderboardInteractionHologramEntity(
    private val leaderboard: MinigameLeaderboard,
    private val lines: List<String>
) : HologramEntity(
    lines.firstOrNull() ?: " ",
    leaderboard.position
        .copy(y = leaderboard.position.y - 3.8)
        .toLocation(Bukkit.getWorlds().first()),
    null
)
{
    init
    {
        persistent = false
        offset = true
    }

    fun configure()
    {
        initializeData()
        EntityHandler.trackEntity(this)
        updateLines(lines)
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
}
