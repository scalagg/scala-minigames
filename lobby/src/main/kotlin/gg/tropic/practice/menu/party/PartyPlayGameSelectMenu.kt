package gg.tropic.practice.menu.party

import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.versioned.MinigameModernAccess
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player

class PartyPlayGameSelectMenu() : Menu("Select a game")
{
    private val robotFightsMaterial = if (MinigameModernAccess.isModern()) {
        Material.matchMaterial("END_PORTAL_FRAME") ?: Material.matchMaterial("ENDER_EYE") ?: Material.NETHER_STAR
    } else {
        Material.ENDER_EYE
    }

    init
    {
        placeholder = true
    }

    override fun getButtons(player: Player) = mapOf(
        3 to ItemBuilder
            .of(Material.matchMaterial("GOLDEN_SWORD") ?: Material.IRON_SWORD)
            .name("${CC.AQUA}Team vs. Team Fights")
            .addToLore(
                "${CC.GRAY}Create two teams with your",
                "${CC.GRAY}party members and fight",
                "${CC.GRAY}against one another!",
                "",
                "${CC.GREEN}Click to play!"
            )
            .toButton { _, _ ->
                val lobbyPlayer = LobbyPlayerService.find(player)
                    ?: return@toButton

                player.closeInventory()
                if (!lobbyPlayer.isInParty())
                {
                    player.sendMessage("${CC.RED}You are no longer in a party!")
                    return@toButton
                }

                val players = lobbyPlayer.partyOf().onlinePlayers()
                if (players.size < 2)
                {
                    player.sendMessage("${CC.RED}You must have at least two players in your party to start a Team vs. Team fight!")
                    return@toButton
                }

                PartyPlayTVTFights(players.toList()).openMenu(player)
            },
        5 to ItemBuilder
            .of(robotFightsMaterial)
            .name("${CC.GOLD}Robot Fights")
            .addToLore(
                "${CC.GRAY}Your team versus bots!",
                "",
                "${CC.GREEN}Click to play!"
            )
            .toButton { _, _ ->
                val lobbyPlayer = LobbyPlayerService.find(player)
                    ?: return@toButton

                player.closeInventory()
                if (!lobbyPlayer.isInParty())
                {
                    player.sendMessage("${CC.RED}You are no longer in a party!")
                    return@toButton
                }

                player.sendMessage("${CC.RED}This feature is currently disabled!")

            }
    )
}
