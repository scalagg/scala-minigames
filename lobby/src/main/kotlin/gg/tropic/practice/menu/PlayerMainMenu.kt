package gg.tropic.practice.menu

import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.parkour.ParkourService
import gg.tropic.practice.menu.editor.EditorKitSelectionMenu
import gg.tropic.practice.versioned.MinigameModernAccess
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class PlayerMainMenu : Menu("Navigator")
{
    init
    {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 27
    private fun menuItem(material: Material, title: String) = ItemBuilder
        .of(material)
        .name(title)
        .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS)

    override fun getButtons(player: Player) = mapOf(
        9 to menuItem(Material.matchMaterial("GRASS_BLOCK") ?: Material.EMERALD_BLOCK, MinigameModernAccess.gradient("Spawn", "#77d76a", "#d6ffb5"))
            .addToLore(
                "${CC.GRAY}Teleport back to spawn!",
                "",
                "${CC.GREEN}Click to teleport!"
            )
            .toButton { _, _ ->
                player.performCommand("spawn")
            },
        11 to menuItem(MinigameModernAccess.duelsCosmeticsMaterial(), MinigameModernAccess.gradient("Cosmetics", "#d38dff", "#ffd8f4"))
            .addToLore(
                "${CC.GRAY}Customize everything about",
                "${CC.GRAY}your in-game profile using",
                "${CC.GRAY}the cosmetics menu!",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                player.performCommand("cosmetics")
            },
        13 to menuItem(MinigameModernAccess.loadoutMaterial(), MinigameModernAccess.gradient("Kit Editor", "#71d6ff", "#d6fbff"))
            .addToLore(
                "${CC.GRAY}Edit your kit loadouts",
                "${CC.GRAY}from one place in the",
                "${CC.GRAY}navigator menu!",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                val profile = PracticeProfileService.find(player)
                    ?: return@toButton

                EditorKitSelectionMenu(player, profile).openMenu(player)
                MinigameModernAccess.playMenuSound(player)
            },
        15 to menuItem(Material.matchMaterial("SPYGLASS") ?: Material.PAPER, MinigameModernAccess.gradient("Statistics", "#b389ff", "#e4d2ff"))
            .addToLore(
                "${CC.GRAY}View network and duel",
                "${CC.GRAY}statistics in one place.",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                player.performCommand("statistics")
            },
        17 to menuItem(Material.matchMaterial("LADDER") ?: Material.PAPER, MinigameModernAccess.gradient("Parkour", "#ffcb68", "#fff0b2"))
            .addToLore(
                "${CC.GRAY}Navigate to the parkour",
                "${CC.GRAY}area! Try to get on the",
                "${CC.GRAY}leaderboards!",
                "",
                *ParkourService.topTenLeaderboardEntries.toTypedArray(),
                "",
                "${CC.GREEN}Click to teleport!"
            )
            .toButton { _, _ ->
                player.performCommand("parkour")
            },
    )
}
