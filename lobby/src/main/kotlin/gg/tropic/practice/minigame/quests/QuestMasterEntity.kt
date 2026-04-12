package gg.tropic.practice.minigame.quests

import gg.tropic.practice.menu.quests.PlayerFacingQuestsMenu
import net.evilblock.cubed.entity.villager.VillagerEntity
import net.evilblock.cubed.util.CC
import org.bukkit.Location
import org.bukkit.entity.Player

class QuestMasterEntity(
    location: Location
) : VillagerEntity(
    listOf(
        "${CC.AQUA}Quest Master",
        "${CC.B_YELLOW}RIGHT CLICK"
    ),
    location
)
{
    init
    {
        persistent = false
    }

    override fun onLeftClick(player: Player)
    {
        PlayerFacingQuestsMenu().openMenu(player)
    }

    override fun onRightClick(player: Player)
    {
        PlayerFacingQuestsMenu().openMenu(player)
    }
}
