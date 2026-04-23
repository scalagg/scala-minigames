package gg.tropic.practice.menu.cosmetic.duels

import gg.tropic.practice.menu.cosmetic.TemplateCosmeticItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

data class DuelsCosmeticItem(
    override val id: String,
    override val displayName: String,
    override val description: List<String>,
    override val material: Material,
    override val materialData: Short = 0,
    override val skullTexture: String? = null,
    override val rarityDisplayName: String,
    override val rarityColorCode: String,
    override val raritySortOrder: Int,
    override val cost: Long,
    private val iconBuilder: (() -> ItemStack)? = null
) : TemplateCosmeticItem
{
    override fun displayItem(): ItemStack
    {
        return iconBuilder?.invoke() ?: ItemStack(material, 1, materialData)
    }
}
