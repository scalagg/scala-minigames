package gg.tropic.practice.menu.cosmetic.duels

import gg.tropic.practice.cosmetic.DuelsCosmeticRegistry
import gg.tropic.practice.cosmetic.DuelsCosmeticRuntime
import gg.tropic.practice.menu.cosmetic.TemplateCosmeticGroup
import gg.tropic.practice.profile.PracticeProfileService
import org.bukkit.Material
import org.bukkit.entity.Player

enum class DuelsCosmeticGroup(
    override val groupId: String,
    override val displayName: String,
    override val description: List<String>,
    override val iconMaterial: Material,
    override val menuSlot: Int,
    private val cosmeticEntries: List<DuelsCosmeticItem>
) : TemplateCosmeticGroup
{
    ARMOR_TRIMS(
        groupId = "armor-trims",
        displayName = "Armor Trim Cosmetics",
        description = listOf(
            "§7Unlock custom trim styles for",
            "§7your duel armor sets."
        ),
        iconMaterial = Material.matchMaterial("NETHERITE_CHESTPLATE") ?: Material.DIAMOND_CHESTPLATE,
        menuSlot = 20,
        cosmeticEntries = DuelsCosmeticRegistry.armorTrimCosmetics.map { entry ->
            DuelsCosmeticItem(
                id = entry.id,
                displayName = entry.displayName,
                description = entry.description,
                material = entry.previewMaterial,
                rarityDisplayName = entry.rarity.displayName,
                rarityColorCode = entry.rarity.colorCode,
                raritySortOrder = entry.rarity.sortOrder,
                cost = entry.cost,
                iconBuilder = {
                    DuelsCosmeticRuntime.createTrimPreview(
                        entry.previewMaterial,
                        entry.trimMaterialKey,
                        entry.trimPatternKey
                    )
                }
            )
        }
    ),
    SHIELD_BANNERS(
        groupId = "shield-banners",
        displayName = "Shield Cosmetics",
        description = listOf(
            "§7Equip banner patterns on shields",
            "§7for your duel loadouts."
        ),
        iconMaterial = Material.matchMaterial("SHIELD") ?: Material.WHITE_BANNER,
        menuSlot = 24,
        cosmeticEntries = DuelsCosmeticRegistry.shieldBannerCosmetics.map { entry ->
            DuelsCosmeticItem(
                id = entry.id,
                displayName = entry.displayName,
                description = entry.description,
                material = Material.matchMaterial("SHIELD") ?: Material.WHITE_BANNER,
                rarityDisplayName = entry.rarity.displayName,
                rarityColorCode = entry.rarity.colorCode,
                raritySortOrder = entry.rarity.sortOrder,
                cost = entry.cost,
                iconBuilder = {
                    DuelsCosmeticRuntime.createShieldPreview(entry.patterns)
                }
            )
        }
    );

    override val iconData: Short = 0
    override val previewable: Boolean = false
    override val skullTexture: String? = null

    override fun entries() = cosmeticEntries

    override fun selectedId(player: Player): String
    {
        return PracticeProfileService.find(player)?.selectedDuelsCosmetic(groupId) ?: "none"
    }

    override fun select(player: Player, id: String)
    {
        PracticeProfileService.find(player)?.selectDuelsCosmetic(groupId, id)
    }

    override fun ownedIds(player: Player): Set<String>
    {
        return PracticeProfileService.find(player)?.unlockedDuelsCosmetics(groupId) ?: setOf("none")
    }

    override fun ownedCount(player: Player) = ownedIds(player).size

    override fun unlock(player: Player, id: String): Boolean
    {
        val profile = PracticeProfileService.find(player) ?: return false
        return profile.unlockedDuelsCosmetics(groupId).add(id)
    }

    override fun saveProfile(player: Player)
    {
        PracticeProfileService.find(player)?.save()
    }

    companion object
    {
        fun allGroups() = entries.toList()
    }
}
