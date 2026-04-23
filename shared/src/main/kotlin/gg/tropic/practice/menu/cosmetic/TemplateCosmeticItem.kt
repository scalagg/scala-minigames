package gg.tropic.practice.menu.cosmetic

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * @author Limo
 * @date 2/21/2026
 */
interface TemplateCosmeticItem
{
    val id: String
    val displayName: String
    val description: List<String>
    val material: Material
    val materialData: Short
    val skullTexture: String?
    val rarityDisplayName: String
    val rarityColorCode: String
    val raritySortOrder: Int
    val cost: Long

    fun displayItem(): ItemStack = ItemStack(material, 1, materialData)

    fun isFree(): Boolean = cost <= 0L

    fun buildLore(
        categoryName: String,
        isSelected: Boolean,
        isOwned: Boolean,
        previewable: Boolean = false
    ): List<String>
    {
        val lore = mutableListOf<String>()
        lore += "\u00a78$categoryName"
        lore += ""
        lore += description
        lore += ""

        if (previewable && rarityDisplayName != "None")
        {
            lore += "\u00a7eRight-Click to preview!"
            lore += ""
        }

        lore += "\u00a77Rarity: $rarityColorCode$rarityDisplayName"

        if (!isFree() && !isOwned)
        {
            lore += "\u00a77Cost: \u00a76${net.evilblock.cubed.util.math.Numbers.format(cost)} coins"
        }

        lore += ""

        if (isSelected)
        {
            lore += "\u00a7aSelected"
            lore += "\u00a7eClick to deselect!"
        }
        else if (isOwned || isFree())
        {
            lore += "\u00a7aUnlocked"
            lore += "\u00a7eClick to select!"
        }
        else
        {
            lore += "\u00a7cLocked"
            lore += "\u00a7eClick to purchase!"
        }

        return lore
    }
}
