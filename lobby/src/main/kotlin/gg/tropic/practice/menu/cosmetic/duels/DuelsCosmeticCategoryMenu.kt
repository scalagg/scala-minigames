package gg.tropic.practice.menu.cosmetic.duels

import gg.scala.game.extensions.economy.EconomyProfileService
import gg.tropic.practice.menu.cosmetic.TemplateCosmeticCategoryMenu
import gg.tropic.practice.menu.cosmetic.TemplateCosmeticGroup
import gg.tropic.practice.menu.cosmetic.TemplateCosmeticItem
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class DuelsCosmeticCategoryMenu(
    group: DuelsCosmeticGroup,
    page: Int = 0,
    searchQuery: String = "",
    sortMode: SortMode = SortMode.LOWEST_RARITY_FIRST,
    ownedItemsFirst: Boolean = false
) : TemplateCosmeticCategoryMenu(group, page, searchQuery, sortMode, ownedItemsFirst)
{
    private fun duelsGroup() = group() as DuelsCosmeticGroup

    override fun getBalance(player: Player) = EconomyProfileService.find(player)?.balance("coins") ?: 0L
    override fun balanceLabel() = "Coins"

    override fun openSearchSign(player: Player, initialQuery: String, callback: (String) -> Unit)
    {
        DuelsCosmeticSignInput.openSearch(player, initialQuery, callback)
    }

    override fun openMainMenu(player: Player)
    {
        DuelsCosmeticsMenu().openMenu(player)
    }

    override fun openSelf(player: Player, newPage: Int, newSearchQuery: String, newSortMode: SortMode, newOwnedItemsFirst: Boolean)
    {
        DuelsCosmeticCategoryMenu(duelsGroup(), newPage, newSearchQuery, newSortMode, newOwnedItemsFirst).openMenu(player)
    }

    override fun attemptPurchase(player: Player, group: TemplateCosmeticGroup, item: TemplateCosmeticItem, cost: Long, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    {
        val economyProfile = EconomyProfileService.find(player)
        if (economyProfile == null)
        {
            onFailure("${CC.RED}Could not load your coin balance.")
            return
        }

        if (!economyProfile.removeBalance("coins", cost))
        {
            onFailure("${CC.RED}You need ${CC.GOLD}${Numbers.format(cost)} coins${CC.RED} to buy this cosmetic.")
            return
        }

        EconomyProfileService.save(economyProfile)
        onSuccess()
    }

    override fun getButtons(player: Player): MutableMap<Int, Button>
    {
        val buttons = super.getButtons(player)
        val selectedId = duelsGroup().selectedId(player)
        val selected = duelsGroup().entries().firstOrNull { it.id == selectedId } ?: duelsGroup().entries().first()

        val currentPreview = if (selected.id == "none") {
            ItemBuilder.of(duelsGroup().iconMaterial)
        } else {
            ItemBuilder.copyOf(selected.displayItem())
        }

        buttons[4] = currentPreview
            .name("${CC.YELLOW}Current ${duelsGroup().displayName.removeSuffix(" Cosmetics")}")
            .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS)
            .addToLore(
                if (selected.id == "none") "${CC.GRAY}Nothing equipped" else "${CC.GRAY}${selected.displayName}",
                "",
                "${CC.GRAY}Select a cosmetic below to",
                "${CC.GRAY}equip it for your duel matches."
            )
            .toButton { _, _ -> }

        return buttons
    }
}
