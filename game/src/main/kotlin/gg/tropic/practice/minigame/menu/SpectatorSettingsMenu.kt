package gg.tropic.practice.minigame.menu

import com.cryptomorin.xseries.XMaterial
import gg.tropic.practice.minigame.spectators.SpectatorSettingsService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

class SpectatorSettingsMenu : Menu("Spectator Settings")
{
    init
    {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 27

    private fun state(enabled: Boolean) = if (enabled) "${CC.GREEN}Enabled" else "${CC.RED}Disabled"

    override fun getButtons(player: Player): Map<Int, Button>
    {
        SpectatorSettingsService.preload(player)

        val nightVision = SpectatorSettingsService.isNightVisionEnabled(player.uniqueId)
        val tracking = SpectatorSettingsService.isTrackingEnabled(player.uniqueId)
        val firstPerson = SpectatorSettingsService.isFirstPersonEnabled(player.uniqueId)

        return mapOf(
            11 to ItemBuilder
                .of(XMaterial.ENDER_EYE)
                .name("${CC.AQUA}Night Vision")
                .addToLore(
                    "${CC.GRAY}Status: ${state(nightVision)}",
                    "${CC.GRAY}Click to toggle."
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)

                    val enabled = SpectatorSettingsService.toggleNightVision(player)
                    player.sendMessage("${CC.AQUA}Night Vision is now ${if (enabled) "${CC.GREEN}enabled" else "${CC.RED}disabled"}${CC.AQUA}.")
                    SpectatorSettingsMenu().openMenu(player)
                },
            13 to ItemBuilder
                .of(XMaterial.COMPASS)
                .name("${CC.AQUA}Tracking")
                .addToLore(
                    "${CC.GRAY}Status: ${state(tracking)}",
                    "${CC.GRAY}Shows target HUD information",
                    "${CC.GRAY}without locking your camera.",
                    "${CC.GRAY}Click to toggle."
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)

                    val enabled = SpectatorSettingsService.toggleTracking(player)
                    player.sendMessage("${CC.AQUA}Tracking is now ${if (enabled) "${CC.GREEN}enabled" else "${CC.RED}disabled"}${CC.AQUA}.")
                    SpectatorSettingsMenu().openMenu(player)
                },
            15 to ItemBuilder
                .of(XMaterial.PLAYER_HEAD)
                .name("${CC.AQUA}First Person View")
                .addToLore(
                    "${CC.GRAY}Status: ${state(firstPerson)}",
                    "${CC.GRAY}You can also right-click players",
                    "${CC.GRAY}while spectating to use this.",
                    "${CC.GRAY}Press sneak to exit first person.",
                    "${CC.GRAY}When enabled, tracking uses",
                    "${CC.GRAY}the target's exact point of view.",
                    "${CC.GRAY}Click to toggle."
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)

                    val enabled = SpectatorSettingsService.toggleFirstPerson(player)
                    player.sendMessage("${CC.AQUA}First Person View is now ${if (enabled) "${CC.GREEN}enabled" else "${CC.RED}disabled"}${CC.AQUA}.")
                    SpectatorSettingsMenu().openMenu(player)
                },
            22 to ItemBuilder
                .of(XMaterial.BARRIER)
                .name("${CC.RED}Clear Tracked Target")
                .addToLore(
                    "${CC.GRAY}Click to stop tracking",
                    "${CC.GRAY}the player you are spectating."
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)
                    SpectatorSettingsService.clearTrackedTarget(player.uniqueId)
                    player.sendMessage("${CC.RED}Cleared your tracked target.")
                    player.closeInventory()
                }
        )
    }
}
