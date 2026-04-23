package gg.tropic.practice.menu.cosmetic.duels

import gg.tropic.practice.menu.cosmetic.TemplateCosmeticGroup
import gg.tropic.practice.menu.cosmetic.TemplateCosmeticSearchResultsMenu
import org.bukkit.entity.Player

class DuelsCosmeticSearchResultsMenu(query: String, page: Int = 0) : TemplateCosmeticSearchResultsMenu(query, page)
{
    override fun groups(): List<TemplateCosmeticGroup> = DuelsCosmeticGroup.allGroups()

    override fun openSearchSign(player: Player, initialQuery: String, callback: (String) -> Unit)
    {
        DuelsCosmeticSignInput.openSearch(player, initialQuery, callback)
    }

    override fun openMainMenu(player: Player)
    {
        DuelsCosmeticsMenu().openMenu(player)
    }

    override fun openSearchResults(player: Player, query: String, page: Int)
    {
        DuelsCosmeticSearchResultsMenu(query, page).openMenu(player)
    }

    override fun openCategoryMenu(player: Player, group: TemplateCosmeticGroup, searchQuery: String)
    {
        DuelsCosmeticCategoryMenu(group as DuelsCosmeticGroup, searchQuery = searchQuery).openMenu(player)
    }
}
