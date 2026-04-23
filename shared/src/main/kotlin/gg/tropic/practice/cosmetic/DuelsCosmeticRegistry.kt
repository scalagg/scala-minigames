package gg.tropic.practice.cosmetic

import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.banner.Pattern
import org.bukkit.inventory.ItemStack
import java.util.Locale

data class DuelsCosmeticTier(
    val displayName: String,
    val colorCode: String,
    val sortOrder: Int
)

data class DuelsArmorTrimCosmeticDefinition(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val previewMaterial: Material,
    val rarity: DuelsCosmeticTier,
    val cost: Long,
    val trimMaterialKey: String?,
    val trimPatternKey: String?
)

data class DuelsShieldPatternDefinition(
    val color: DyeColor,
    val patternTypeKey: String
)

data class DuelsShieldBannerCosmeticDefinition(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val rarity: DuelsCosmeticTier,
    val cost: Long,
    val patterns: List<DuelsShieldPatternDefinition>
)

object DuelsCosmeticRegistry
{
    val armorTrimPatternKeys = listOf(
        "sentry", "dune", "coast", "wild", "ward", "eye", "vex", "tide", "snout",
        "rib", "spire", "wayfinder", "shaper", "silence", "raiser", "host", "flow", "bolt"
    )

    val armorTrimMaterialKeys = listOf(
        "quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst", "resin"
    )

    val shieldBannerPatternKeys = listOf(
        "SQUARE_BOTTOM_LEFT", "SQUARE_BOTTOM_RIGHT", "SQUARE_TOP_LEFT", "SQUARE_TOP_RIGHT",
        "STRIPE_BOTTOM", "STRIPE_TOP", "STRIPE_LEFT", "STRIPE_RIGHT", "STRIPE_CENTER", "STRIPE_MIDDLE",
        "STRIPE_DOWNRIGHT", "STRIPE_DOWNLEFT", "SMALL_STRIPES", "CROSS", "STRAIGHT_CROSS", "TRIANGLE_BOTTOM",
        "TRIANGLE_TOP", "TRIANGLES_BOTTOM", "TRIANGLES_TOP", "DIAGONAL_LEFT", "DIAGONAL_RIGHT", "DIAGONAL_UP_LEFT",
        "DIAGONAL_UP_RIGHT", "CIRCLE_MIDDLE", "RHOMBUS_MIDDLE", "HALF_VERTICAL", "HALF_HORIZONTAL",
        "HALF_VERTICAL_RIGHT", "HALF_HORIZONTAL_BOTTOM", "BORDER", "CURLY_BORDER", "CREEPER", "GRADIENT",
        "GRADIENT_UP", "BRICKS", "SKULL", "FLOWER", "MOJANG", "GLOBE", "PIGLIN", "FLOW", "GUSTER"
    )

    private val defaultArmorPreviewMaterial = Material.matchMaterial("NETHERITE_CHESTPLATE")
        ?: Material.matchMaterial("DIAMOND_CHESTPLATE")
        ?: Material.IRON_CHESTPLATE

    private val commonTier = DuelsCosmeticTier("Common", "\u00a7a", 4)
    private val rareTier = DuelsCosmeticTier("Rare", "\u00a7b", 3)
    private val epicTier = DuelsCosmeticTier("Epic", "\u00a7d", 2)
    private val legendaryTier = DuelsCosmeticTier("Legendary", "\u00a76", 1)
    private val mythicTier = DuelsCosmeticTier("Mythic", "\u00a7c", 0)
    private val noneTier = DuelsCosmeticTier("None", "\u00a77", 99)

    private val builtInArmorTrimCosmetics: List<DuelsArmorTrimCosmeticDefinition> = buildArmorTrimCosmetics()
    private val builtInShieldBannerCosmetics: List<DuelsShieldBannerCosmeticDefinition> = buildShieldBannerCosmetics()

    val armorTrimCosmetics: List<DuelsArmorTrimCosmeticDefinition>
        get() = builtInArmorTrimCosmetics + runCatching { DuelsCustomCosmeticService.cached().trims.toList() }.getOrDefault(emptyList())

    val shieldBannerCosmetics: List<DuelsShieldBannerCosmeticDefinition>
        get() = builtInShieldBannerCosmetics + runCatching { DuelsCustomCosmeticService.cached().shieldBanners.toList() }.getOrDefault(emptyList())

    fun armorTrimById(id: String) = armorTrimCosmetics.firstOrNull { it.id == id }
    fun shieldBannerById(id: String) = shieldBannerCosmetics.firstOrNull { it.id == id }
    fun builtInArmorTrimById(id: String) = builtInArmorTrimCosmetics.firstOrNull { it.id.equals(id, true) }
    fun builtInShieldBannerById(id: String) = builtInShieldBannerCosmetics.firstOrNull { it.id.equals(id, true) }
    fun anyCosmeticIdExists(id: String) = armorTrimById(id) != null || shieldBannerById(id) != null
    fun defaultTrimDisplayName(id: String) = "${prettyName(id)} Trim"
    fun defaultBannerDisplayName(id: String) = "${prettyName(id)} Banner"

    private fun buildArmorTrimCosmetics(): List<DuelsArmorTrimCosmeticDefinition>
    {
        val patternKeys = armorTrimPatternKeys
        val trimMaterialKeys = listOf(
            "iron", "gold", "diamond", "emerald", "lapis", "redstone", "amethyst", "copper", "quartz",
            "netherite", "iron", "gold", "diamond", "amethyst", "emerald", "redstone", "resin", "netherite"
        )

        return listOf(
            DuelsArmorTrimCosmeticDefinition(
                id = "none",
                displayName = "No Trim",
                description = listOf(
                    "\u00a77Fight with the default armor look."
                ),
                previewMaterial = defaultArmorPreviewMaterial,
                rarity = noneTier,
                cost = 0,
                trimMaterialKey = null,
                trimPatternKey = null
            )
        ) + patternKeys.mapIndexed { index, patternKey ->
            val displayName = prettyName(patternKey)
            DuelsArmorTrimCosmeticDefinition(
                id = "$patternKey-trim",
                displayName = "$displayName Trim",
                description = listOf(
                    "\u00a77Unlock the $displayName armor trim",
                    "\u00a77for your duel loadouts."
                ),
                previewMaterial = defaultArmorPreviewMaterial,
                rarity = tierFor(index, patternKeys.size),
                cost = 750L + (index * 350L),
                trimMaterialKey = trimMaterialKeys[index],
                trimPatternKey = patternKey
            )
        }
    }

    private fun buildShieldBannerCosmetics(): List<DuelsShieldBannerCosmeticDefinition>
    {
        val patternKeys = shieldBannerPatternKeys

        return listOf(
            DuelsShieldBannerCosmeticDefinition(
                id = "none",
                displayName = "No Banner",
                description = listOf(
                    "\u00a77Fight with a clean shield."
                ),
                rarity = noneTier,
                cost = 0,
                patterns = emptyList()
            )
        ) + patternKeys.mapIndexed { index, patternKey ->
            val displayName = prettyName(patternKey)
            DuelsShieldBannerCosmeticDefinition(
                id = "banner-${patternKey.lowercase(Locale.ROOT).replace('_', '-')}",
                displayName = "$displayName Banner",
                description = listOf(
                    "\u00a77Equip the $displayName banner",
                    "\u00a77on your duel shield."
                ),
                rarity = tierFor(index, patternKeys.size),
                cost = 150L + (index * 90L),
                patterns = listOf(
                    DuelsShieldPatternDefinition(
                        color = shieldPatternColor(patternKey),
                        patternTypeKey = patternKey
                    )
                )
            )
        }
    }

    private fun tierFor(index: Int, total: Int): DuelsCosmeticTier
    {
        val ratio = if (total <= 1) 0.0 else index.toDouble() / (total - 1).toDouble()
        return when {
            ratio >= 0.85 -> mythicTier
            ratio >= 0.65 -> legendaryTier
            ratio >= 0.4 -> epicTier
            ratio >= 0.2 -> rareTier
            else -> commonTier
        }
    }

    fun prettyName(raw: String): String
    {
        return raw
            .lowercase(Locale.ROOT)
            .split('_')
            .joinToString(" ") { part ->
                part.replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase(Locale.ROOT) else character.toString()
                }
            }
    }

    private fun shieldPatternColor(patternKey: String): DyeColor
    {
        return when (patternKey) {
            "CREEPER" -> DyeColor.GREEN
            "FLOWER" -> DyeColor.RED
            "BRICKS" -> DyeColor.BROWN
            "MOJANG" -> DyeColor.PURPLE
            "GLOBE" -> DyeColor.CYAN
            "PIGLIN" -> DyeColor.BLACK
            "FLOW" -> DyeColor.LIME
            "GUSTER" -> DyeColor.GRAY
            "GRADIENT", "GRADIENT_UP" -> DyeColor.GRAY
            else -> DyeColor.BLACK
        }
    }
}

object DuelsCosmeticRuntime
{
    private val shieldMaterial = Material.matchMaterial("SHIELD")

    fun createTrimPreview(material: Material, trimMaterialKey: String?, trimPatternKey: String?): ItemStack
    {
        val item = ItemStack(material)
        if (trimMaterialKey == null || trimPatternKey == null) {
            return item
        }

        applyTrim(item, trimMaterialKey, trimPatternKey)
        return item
    }

    fun createShieldPreview(patterns: List<DuelsShieldPatternDefinition>): ItemStack
    {
        val item = ItemStack(shieldMaterial ?: Material.BANNER)
        if (patterns.isEmpty()) {
            return item
        }

        applyShield(item, patterns)
        return item
    }

    fun applyTrim(item: ItemStack, trimMaterialKey: String, trimPatternKey: String): Boolean
    {
        val meta = item.itemMeta ?: return false

        return kotlin.runCatching {
            val armorMetaClass = Class.forName("org.bukkit.inventory.meta.ArmorMeta")
            if (!armorMetaClass.isInstance(meta)) return false

            val trimClass = Class.forName("org.bukkit.inventory.meta.trim.ArmorTrim")
            val trimMaterialClass = Class.forName("org.bukkit.inventory.meta.trim.TrimMaterial")
            val trimPatternClass = Class.forName("org.bukkit.inventory.meta.trim.TrimPattern")
            val registryClass = Class.forName("org.bukkit.Registry")
            val namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey")
            val minecraftMethod = namespacedKeyClass.getMethod("minecraft", String::class.java)
            val trimMaterialRegistry = registryClass.getField("TRIM_MATERIAL").get(null)
            val trimPatternRegistry = registryClass.getField("TRIM_PATTERN").get(null)
            val getMethod = trimMaterialRegistry.javaClass.getMethod("get", namespacedKeyClass)
            val trimMaterial = getMethod.invoke(trimMaterialRegistry, minecraftMethod.invoke(null, trimMaterialKey))
                ?: return false
            val trimPattern = getMethod.invoke(trimPatternRegistry, minecraftMethod.invoke(null, trimPatternKey))
                ?: return false
            val trim = trimClass.getConstructor(trimMaterialClass, trimPatternClass).newInstance(trimMaterial, trimPattern)

            armorMetaClass.getMethod("setTrim", trimClass).invoke(meta, trim)
            item.itemMeta = meta
            true
        }.getOrDefault(false)
    }

    fun applyShield(item: ItemStack, definitions: List<DuelsShieldPatternDefinition>): Boolean
    {
        if (shieldMaterial == null || item.type != shieldMaterial) {
            return false
        }

        val meta = item.itemMeta ?: return false

        return kotlin.runCatching {
            val blockStateMetaClass = Class.forName("org.bukkit.inventory.meta.BlockStateMeta")
            if (!blockStateMetaClass.isInstance(meta)) return false

            val blockState = blockStateMetaClass.getMethod("getBlockState").invoke(meta)
            val bannerClass = Class.forName("org.bukkit.block.Banner")
            if (!bannerClass.isInstance(blockState)) return false

            val resolvedPatterns = definitions.mapNotNull { definition ->
                kotlin.runCatching {
                    val patternTypeClass = Class.forName("org.bukkit.block.banner.PatternType")
                    val patternType = patternTypeClass.getMethod("valueOf", String::class.java)
                        .invoke(null, definition.patternTypeKey)
                    Pattern(definition.color, patternType as org.bukkit.block.banner.PatternType)
                }.getOrNull()
            }

            bannerClass.getMethod("setBaseColor", DyeColor::class.java).invoke(blockState, DyeColor.WHITE)
            bannerClass.getMethod("setPatterns", MutableList::class.java).invoke(blockState, resolvedPatterns.toMutableList())
            bannerClass.getMethod("update").invoke(blockState)
            blockStateMetaClass
                .getMethod("setBlockState", Class.forName("org.bukkit.block.BlockState"))
                .invoke(meta, blockState)
            item.itemMeta = meta
            true
        }.getOrDefault(false)
    }
}
