package gg.tropic.practice.minigame.hologram.settings

import com.cryptomorin.xseries.XMaterial
import gg.tropic.practice.configuration.minigame.MinigameLeaderboard
import gg.tropic.practice.minigame.hologram.MinigameLeaderboardHologramEntity
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

/**
 * @author Limo
 * @date 2/22/2026
 */
class MinigameLeaderboardSettingsMenu(
    private val leaderboard: MinigameLeaderboard,
    private val settings: MinigameLeaderboardSettings
) : Menu("Leaderboard Settings")
{
    init
    {
        placeholder = true
    }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val resolved = MinigameLeaderboardSettingsService.resolve(leaderboard, settings)
        val currentMode = resolved.modeOptions.firstOrNull { it.key == settings.mode }
            ?: resolved.modeOptions.first()
        val currentTime = resolved.timeOptions.firstOrNull { it == settings.time }
            ?: resolved.timeOptions.first()

        settings.mode = currentMode.key
        settings.time = currentTime

        return mapOf(
            10 to ItemBuilder
                .of(XMaterial.RED_BED)
                .name("${CC.GREEN}Select the Mode!")
                .addToLore(
                    "",
                    *resolved.modeOptions.map { option ->
                        "${if (option == currentMode) "${CC.GREEN}► ${CC.WHITE}" else CC.GRAY}${option.displayName}"
                    }.toTypedArray(),
                    "",
                    "${CC.GRAY}This setting will save across lobbies.",
                    "${CC.GRAY}Leaderboard data is cached and",
                    "${CC.GRAY}does not update immediately.",
                    "${CC.YELLOW}Left/Right Click to change!"
                )
                .toButton { clicker, click ->
                    val next = cycle(
                        options = resolved.modeOptions,
                        current = currentMode,
                        forward = click?.isRightClick != true
                    )

                    settings.mode = next.key
                    MinigameLeaderboardSettingsMenu(leaderboard, settings).openMenu(clicker!!)
                },
            11 to ItemBuilder
                .of(XMaterial.CLOCK)
                .name("${CC.GREEN}Select the Time!")
                .addToLore(
                    "",
                    *resolved.timeOptions.map { option ->
                        "${if (option == currentTime) "${CC.GREEN}► ${CC.WHITE}" else CC.GRAY}${option.displayName}"
                    }.toTypedArray(),
                    "",
                    "${CC.GRAY}This setting will save across lobbies.",
                    "${CC.GRAY}Leaderboard data is cached and",
                    "${CC.GRAY}does not update immediately.",
                    "${CC.YELLOW}Left/Right Click to change!"
                )
                .toButton { clicker, click ->
                    val next = cycle(
                        options = resolved.timeOptions,
                        current = currentTime,
                        forward = click?.isRightClick != true
                    )

                    settings.time = next
                    MinigameLeaderboardSettingsMenu(leaderboard, settings).openMenu(clicker!!)
                },
            12 to ItemBuilder
                .of(XMaterial.LADDER)
                .name("${CC.GREEN}Select the View!")
                .addToLore(
                    "",
                    *LeaderboardViewOption.entries.map { option ->
                        "${if (option == settings.view) "${CC.GREEN}► ${CC.WHITE}" else CC.GRAY}${option.displayName}"
                    }.toTypedArray(),
                    "",
                    "${CC.GRAY}Leaderboard data is cached and",
                    "${CC.GRAY}does not update immediately.",
                    "${CC.YELLOW}Left/Right Click to change!"
                )
                .toButton { clicker, click ->
                    settings.view = cycle(
                        options = LeaderboardViewOption.entries,
                        current = settings.view,
                        forward = click?.isRightClick != true
                    )

                    MinigameLeaderboardSettingsMenu(leaderboard, settings).openMenu(clicker!!)
                },
            13 to ItemBuilder
                .of(XMaterial.PLAYER_HEAD)
                .name("${CC.GREEN}Select the Players!")
                .addToLore(
                    "",
                    *LeaderboardPlayersOption.entries.map { option ->
                        "${if (option == settings.players) "${CC.GREEN}► ${CC.WHITE}" else CC.GRAY}${option.displayName}"
                    }.toTypedArray(),
                    "",
                    "${CC.GRAY}Leaderboard data is cached and",
                    "${CC.GRAY}does not update immediately.",
                    "${CC.YELLOW}Left/Right Click to change!"
                )
                .toButton { clicker, click ->
                    settings.players = cycle(
                        options = LeaderboardPlayersOption.entries,
                        current = settings.players,
                        forward = click?.isRightClick != true
                    )

                    MinigameLeaderboardSettingsMenu(leaderboard, settings).openMenu(clicker!!)
                },
            14 to ItemBuilder
                .of(XMaterial.ITEM_FRAME)
                .name("${CC.GREEN}Select the Text Alignment!")
                .addToLore(
                    "",
                    *LeaderboardAlignmentOption.entries.map { option ->
                        "${if (option == settings.alignment) "${CC.GREEN}► ${CC.WHITE}" else CC.GRAY}${option.displayName}"
                    }.toTypedArray(),
                    "",
                    "${CC.RED}Block alignment is showing correctly",
                    "${CC.RED}only for Vanilla Minecraft font sizes.",
                    "${CC.GRAY}This setting will save across the network.",
                    "${CC.GRAY}Leaderboard data is cached and",
                    "${CC.GRAY}does not update immediately.",
                    "${CC.YELLOW}Left/Right Click to change!"
                )
                .toButton { clicker, click ->
                    settings.alignment = cycle(
                        options = LeaderboardAlignmentOption.entries,
                        current = settings.alignment,
                        forward = click?.isRightClick != true
                    )

                    MinigameLeaderboardSettingsMenu(leaderboard, settings).openMenu(clicker!!)
                },
            22 to ItemBuilder
                .of(XMaterial.LIME_TERRACOTTA)
                .name("${CC.GREEN}Apply changes")
                .addToLore("${CC.YELLOW}Click to apply the changes!")
                .toButton { clicker, _ ->
                    clicker ?: return@toButton

                    MinigameLeaderboardSettingsService
                        .save(clicker.uniqueId, leaderboard, settings)
                        .join()

                    EntityHandler
                        .getEntitiesByType(MinigameLeaderboardHologramEntity::class.java)
                        .forEach { hologram ->
                            hologram.updateLinesForAll()
                        }

                    clicker.closeInventory()
                    clicker.sendMessage("${CC.GREEN}Leaderboard settings updated.")
                },
            23 to ItemBuilder
                .of(XMaterial.RED_TERRACOTTA)
                .name("${CC.RED}Discard changes")
                .addToLore("${CC.YELLOW}Close the menu without applying changes!")
                .toButton { clicker, _ ->
                    clicker?.closeInventory()
                }
        )
    }

    override fun size(buttons: Map<Int, Button>) = 36

    private fun <T> cycle(options: List<T>, current: T, forward: Boolean): T
    {
        if (options.size == 1)
        {
            return options.first()
        }

        val index = options.indexOf(current)
            .takeIf { it >= 0 }
            ?: 0

        val next = if (forward)
        {
            (index + 1) % options.size
        }
        else
        {
            (index - 1 + options.size) % options.size
        }

        return options[next]
    }
}
