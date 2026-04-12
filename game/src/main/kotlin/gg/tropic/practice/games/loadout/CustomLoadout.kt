package gg.tropic.practice.games.loadout

import gg.tropic.practice.kit.Kit
import gg.tropic.practice.cosmetic.DuelsCosmeticApplicator
import gg.tropic.practice.profile.loadout.Loadout
import gg.tropic.practice.extensions.deepClone
import org.bukkit.entity.Player

class CustomLoadout(
    private val loadout: Loadout,
    private val kit: Kit
) : SelectedLoadout
{
    override fun displayName() = loadout.name
    override fun apply(player: Player)
    {
        kit.populate(player)
        player.inventory.contents = loadout.inventoryContents.deepClone()
        DuelsCosmeticApplicator.apply(player)
        player.fixInventoryBasedOnRBTeamColor()
        player.updateInventory()
    }
}
