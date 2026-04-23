package gg.tropic.practice.menu.cosmetic.duels

import gg.scala.game.extensions.economy.EconomyProfileService
import gg.tropic.practice.menu.cosmetic.TemplateCosmeticGroup
import gg.tropic.practice.menu.cosmetic.TemplateCosmeticsMenu
import org.bukkit.entity.Player

class DuelsCosmeticsMenu : TemplateCosmeticsMenu()
{
    override fun groups(): List<TemplateCosmeticGroup> = DuelsCosmeticGroup.allGroups()
    override fun getBalance(player: Player) = EconomyProfileService.find(player)?.balance("coins") ?: 0L
    override fun balanceLabel() = "Coins"
    override fun backLabel() = "Back to Duels"

    override fun openSearchSign(player: Player, callback: (String) -> Unit)
    {
        DuelsCosmeticSignInput.openSearch(player, action = callback)
    }

    override fun openSearchResults(player: Player, query: String)
    {
        DuelsCosmeticSearchResultsMenu(query).openMenu(player)
    }

    override fun openSelf(player: Player)
    {
        DuelsCosmeticsMenu().openMenu(player)
    }

    override fun onGroupClick(player: Player, group: TemplateCosmeticGroup)
    {
        DuelsCosmeticCategoryMenu(group as DuelsCosmeticGroup).openMenu(player)
    }
}
