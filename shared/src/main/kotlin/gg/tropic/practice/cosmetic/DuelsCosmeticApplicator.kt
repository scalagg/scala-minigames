package gg.tropic.practice.cosmetic

import gg.tropic.practice.profile.PracticeProfileService
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object DuelsCosmeticApplicator
{
    fun apply(player: Player)
    {
        val profile = PracticeProfileService.find(player) ?: return
        applyTrim(profile.selectedDuelsCosmetic("armor-trims"), player.inventory.armorContents)
        applyShield(profile.selectedDuelsCosmetic("shield-banners"), player.inventory.contents)
    }

    private fun applyTrim(selection: String, armorContents: Array<ItemStack?>)
    {
        val definition = DuelsCosmeticRegistry.armorTrimById(selection)
            ?: return
        val trimMaterialKey = definition.trimMaterialKey ?: return
        val trimPatternKey = definition.trimPatternKey ?: return

        armorContents.indices.forEach { index ->
            val item = armorContents[index] ?: return@forEach
            if (!item.type.name.endsWith("HELMET") && !item.type.name.endsWith("CHESTPLATE") && !item.type.name.endsWith("LEGGINGS") && !item.type.name.endsWith("BOOTS")) {
                return@forEach
            }

            DuelsCosmeticRuntime.applyTrim(item, trimMaterialKey, trimPatternKey)
        }
    }

    private fun applyShield(selection: String, contents: Array<ItemStack?>)
    {
        val definition = DuelsCosmeticRegistry.shieldBannerById(selection)
            ?: return

        contents.indices.forEach { index ->
            val item = contents[index] ?: return@forEach
            if (item.type != (Material.matchMaterial("SHIELD") ?: return)) return@forEach

            if (definition.patterns.isEmpty()) {
                return@forEach
            }

            DuelsCosmeticRuntime.applyShield(item, definition.patterns)
        }
    }
}
