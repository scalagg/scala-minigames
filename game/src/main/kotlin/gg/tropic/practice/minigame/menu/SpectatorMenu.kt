package gg.tropic.practice.minigame.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.staff.reports.ReportMenu
import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.event.SpectatorViewParticipantsEvent
import gg.tropic.practice.minigame.spectators.SpectatorSettingsService
import gg.tropic.practice.minigame.spectators.PlayerParticipant
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

class SpectatorMenu(
    private val player: Player,
    minigame: GameImpl
) : PaginatedMenu()
{
    private val event = SpectatorViewParticipantsEvent(
        game = minigame,
        viewer = player,
        players = minigame.allNonSpectators()
            .map { player ->
                PlayerParticipant(
                    player = player,
                    displayName = "${CC.GREEN}${player.name}",
                    description = listOf(
                        "${CC.GRAY}Health: ${CC.YELLOW}${
                            ((player.health / player.maxHealth) * 100.0)
                                .coerceIn(0.0, 100.0)
                                .toInt()
                        }",
                        "${CC.GRAY}Food: ${CC.YELLOW}${((player.foodLevel / 20.0) * 100.0).toInt()}",
                        ""
                    )
                )
            }
    )

    init
    {
        applyHeader = false
        Bukkit.getPluginManager().callEvent(event)
    }

    override fun getMaxItemsPerPage(player: Player) = 21
    override fun size(buttons: Map<Int, Button>) = 45

    override fun getAllPagesButtonSlots() = (10..16) + (19..25) + (28..34)

    override fun getPrePaginatedTitle(player: Player) = "Teleporter"

    override fun getAllPagesButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        event.players.forEach { other ->
            buttons[buttons.size] = ItemBuilder.of(XMaterial.PLAYER_HEAD)
                .owner(other.player.name)
                .name(other.displayName)
                .setLore(other.description)
                .addToLore(
                    "${CC.GREEN}Left-click to track target info!",
                    "${CC.AQUA}Right-click for first person!",
                    "${CC.RED}Shift-right-click to report!",
                )
                .data(3)
                .toButton { _, type ->
                    Button.playNeutral(player)
                    if (!GameService.isSpectating(player))
                    {
                        player.sendMessage("${CC.RED}You must be a spectator to use the teleporter!")
                        return@toButton
                    }

                    if (!other.player.isOnline)
                    {
                        player.sendMessage("${CC.RED}That player is no longer available to spectate.")
                        return@toButton
                    }

                    if (type?.isRightClick == true && type.isShiftClick)
                    {
                        ReportMenu(other.player.uniqueId).openMenu(player)
                        return@toButton
                    }

                    val firstPerson = type?.isRightClick == true
                    val started = SpectatorSettingsService.startTracking(player, other.player, firstPerson)
                    if (!started)
                    {
                        return@toButton
                    }

                    player.closeInventory()

                    if (firstPerson)
                    {
                        player.teleport(SpectatorSettingsService.firstPersonLocation(player, other.player))
                        player.sendMessage("${CC.GREEN}Now spectating ${CC.WHITE}${other.player.name}${CC.GREEN} in first person.")
                        return@toButton
                    }

                    player.teleport(other.player.location)
                    player.sendMessage("${CC.GREEN}Now tracking ${CC.WHITE}${other.player.name}${CC.GREEN}.")
                }
        }
        return buttons
    }
}
