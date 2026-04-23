package gg.tropic.practice.commands.admin

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Single
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.cosmetic.DuelsArmorTrimCosmeticDefinition
import gg.tropic.practice.cosmetic.DuelsCosmeticRegistry
import gg.tropic.practice.cosmetic.DuelsCosmeticTier
import gg.tropic.practice.cosmetic.DuelsCustomCosmeticService
import gg.tropic.practice.cosmetic.DuelsShieldBannerCosmeticDefinition
import gg.tropic.practice.cosmetic.DuelsShieldPatternDefinition
import net.evilblock.cubed.util.CC
import org.bukkit.DyeColor
import org.bukkit.Material
import java.util.Locale

@AutoRegister
@CommandAlias("duelscosmetic|duelscosmetics|dcosmetic|dcosmetic")
@CommandPermission("practice.command.duelscosmetic")
object DuelsCosmeticAdminCommand : ScalaCommand()
{
    private val customTier = DuelsCosmeticTier("Custom", "${CC.AQUA}", 2)

    @Default
    @HelpCommand
    fun onDefault(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("list")
    @Description("List all duels cosmetics.")
    fun onList(player: ScalaPlayer)
    {
        val trims = DuelsCosmeticRegistry.armorTrimCosmetics
        val banners = DuelsCosmeticRegistry.shieldBannerCosmetics

        player.sendMessage(
            "${CC.GREEN}Duels trim cosmetics: ${CC.WHITE}${trims.size}",
            trims.joinToString("${CC.GRAY}, ") { "${CC.WHITE}${it.id}" },
            "",
            "${CC.GREEN}Duels shield banner cosmetics: ${CC.WHITE}${banners.size}",
            banners.joinToString("${CC.GRAY}, ") { "${CC.WHITE}${it.id}" }
        )
    }

    @Subcommand("trim create")
    @AssignPermission
    @Description("Create a custom duels armor trim cosmetic.")
    fun onTrimCreate(player: ScalaPlayer, @Single id: String, @Single trimMaterialKey: String, @Single trimPatternKey: String, previewMaterial: Material)
    {
        val normalizedId = id.lowercase(Locale.ROOT)
        ensureIdAvailable(normalizedId)

        val container = DuelsCustomCosmeticService.cached()
        container.trims += DuelsArmorTrimCosmeticDefinition(
            id = normalizedId,
            displayName = DuelsCosmeticRegistry.defaultTrimDisplayName(normalizedId),
            description = mutableListOf(
                "${CC.GRAY}Custom duels armor trim.",
                "${CC.GRAY}Edit this cosmetic with /duelscosmetic trim ..."
            ),
            previewMaterial = previewMaterial,
            rarity = customTier,
            cost = 0,
            trimMaterialKey = trimMaterialKey.lowercase(Locale.ROOT),
            trimPatternKey = trimPatternKey.lowercase(Locale.ROOT)
        )
        DuelsCustomCosmeticService.sync(container)

        player.sendMessage("${CC.GREEN}Created duels trim ${CC.YELLOW}$normalizedId${CC.GREEN}.")
    }

    @Subcommand("trim delete")
    @AssignPermission
    @CommandCompletion("@duels-custom-trims")
    @Description("Delete a custom duels armor trim cosmetic.")
    fun onTrimDelete(player: ScalaPlayer, @Single id: String)
    {
        val container = DuelsCustomCosmeticService.cached()
        val trim = container.trimById(id)
            ?: throw ConditionFailedException("No custom duels trim with the ID ${CC.YELLOW}$id${CC.RED} exists.")

        container.trims -= trim
        DuelsCustomCosmeticService.sync(container)
        player.sendMessage("${CC.GREEN}Deleted duels trim ${CC.YELLOW}${trim.id}${CC.GREEN}.")
    }

    @Subcommand("trim displayname")
    @AssignPermission
    @CommandCompletion("@duels-custom-trims")
    fun onTrimDisplayName(player: ScalaPlayer, @Single id: String, name: String)
    {
        val trim = customTrim(id)
        replaceCustomTrim(trim.copy(displayName = name))
        player.sendMessage("${CC.GREEN}Updated trim ${CC.YELLOW}${trim.id}${CC.GREEN} display name.")
    }

    @Subcommand("trim description")
    @AssignPermission
    @CommandCompletion("@duels-custom-trims")
    @Description("Set trim description. Use | to split lines, or none to clear.")
    fun onTrimDescription(player: ScalaPlayer, @Single id: String, description: String)
    {
        val trim = customTrim(id)
        val parsed = parseDescription(description)
        replaceCustomTrim(trim.copy(description = parsed))
        player.sendMessage("${CC.GREEN}Updated trim ${CC.YELLOW}${trim.id}${CC.GREEN} description.")
    }

    @Subcommand("trim cost")
    @AssignPermission
    @CommandCompletion("@duels-custom-trims")
    fun onTrimCost(player: ScalaPlayer, @Single id: String, cost: Long)
    {
        val trim = customTrim(id)
        replaceCustomTrim(trim.copy(cost = cost))
        player.sendMessage("${CC.GREEN}Updated trim ${CC.YELLOW}${trim.id}${CC.GREEN} cost to ${CC.GOLD}$cost${CC.GREEN}.")
    }

    @Subcommand("trim previewmaterial")
    @AssignPermission
    @CommandCompletion("@duels-custom-trims")
    fun onTrimPreviewMaterial(player: ScalaPlayer, @Single id: String, material: Material)
    {
        val trim = customTrim(id)
        replaceCustomTrim(trim.copy(previewMaterial = material))
        player.sendMessage("${CC.GREEN}Updated trim ${CC.YELLOW}${trim.id}${CC.GREEN} preview material to ${CC.WHITE}${material.name}${CC.GREEN}.")
    }

    @Subcommand("trim trimmaterial")
    @AssignPermission
    @CommandCompletion("@duels-custom-trims @duels-trim-materials")
    fun onTrimMaterial(player: ScalaPlayer, @Single id: String, @Single trimMaterialKey: String)
    {
        val trim = customTrim(id)
        replaceCustomTrim(trim.copy(trimMaterialKey = trimMaterialKey.lowercase(Locale.ROOT)))
        player.sendMessage("${CC.GREEN}Updated trim ${CC.YELLOW}${trim.id}${CC.GREEN} material key to ${CC.WHITE}$trimMaterialKey${CC.GREEN}.")
    }

    @Subcommand("trim trimpattern")
    @AssignPermission
    @CommandCompletion("@duels-custom-trims @duels-trim-patterns")
    fun onTrimPattern(player: ScalaPlayer, @Single id: String, @Single trimPatternKey: String)
    {
        val trim = customTrim(id)
        replaceCustomTrim(trim.copy(trimPatternKey = trimPatternKey.lowercase(Locale.ROOT)))
        player.sendMessage("${CC.GREEN}Updated trim ${CC.YELLOW}${trim.id}${CC.GREEN} pattern key to ${CC.WHITE}$trimPatternKey${CC.GREEN}.")
    }

    @Subcommand("banner create")
    @AssignPermission
    @Description("Create a custom duels shield banner cosmetic.")
    fun onBannerCreate(player: ScalaPlayer, @Single id: String)
    {
        val normalizedId = id.lowercase(Locale.ROOT)
        ensureIdAvailable(normalizedId)

        val container = DuelsCustomCosmeticService.cached()
        container.shieldBanners += DuelsShieldBannerCosmeticDefinition(
            id = normalizedId,
            displayName = DuelsCosmeticRegistry.defaultBannerDisplayName(normalizedId),
            description = mutableListOf(
                "${CC.GRAY}Custom duels shield banner.",
                "${CC.GRAY}Add layers with /duelscosmetic banner pattern add"
            ),
            rarity = customTier,
            cost = 0,
            patterns = mutableListOf()
        )
        DuelsCustomCosmeticService.sync(container)

        player.sendMessage("${CC.GREEN}Created duels shield banner ${CC.YELLOW}$normalizedId${CC.GREEN}.")
    }

    @Subcommand("banner delete")
    @AssignPermission
    @CommandCompletion("@duels-custom-banners")
    fun onBannerDelete(player: ScalaPlayer, @Single id: String)
    {
        val container = DuelsCustomCosmeticService.cached()
        val banner = container.shieldBannerById(id)
            ?: throw ConditionFailedException("No custom duels shield banner with the ID ${CC.YELLOW}$id${CC.RED} exists.")

        container.shieldBanners -= banner
        DuelsCustomCosmeticService.sync(container)
        player.sendMessage("${CC.GREEN}Deleted duels shield banner ${CC.YELLOW}${banner.id}${CC.GREEN}.")
    }

    @Subcommand("banner displayname")
    @AssignPermission
    @CommandCompletion("@duels-custom-banners")
    fun onBannerDisplayName(player: ScalaPlayer, @Single id: String, name: String)
    {
        val banner = customBanner(id)
        replaceCustomBanner(banner.copy(displayName = name))
        player.sendMessage("${CC.GREEN}Updated shield banner ${CC.YELLOW}${banner.id}${CC.GREEN} display name.")
    }

    @Subcommand("banner description")
    @AssignPermission
    @CommandCompletion("@duels-custom-banners")
    @Description("Set banner description. Use | to split lines, or none to clear.")
    fun onBannerDescription(player: ScalaPlayer, @Single id: String, description: String)
    {
        val banner = customBanner(id)
        replaceCustomBanner(banner.copy(description = parseDescription(description)))
        player.sendMessage("${CC.GREEN}Updated shield banner ${CC.YELLOW}${banner.id}${CC.GREEN} description.")
    }

    @Subcommand("banner cost")
    @AssignPermission
    @CommandCompletion("@duels-custom-banners")
    fun onBannerCost(player: ScalaPlayer, @Single id: String, cost: Long)
    {
        val banner = customBanner(id)
        replaceCustomBanner(banner.copy(cost = cost))
        player.sendMessage("${CC.GREEN}Updated shield banner ${CC.YELLOW}${banner.id}${CC.GREEN} cost to ${CC.GOLD}$cost${CC.GREEN}.")
    }

    @Subcommand("banner pattern add")
    @AssignPermission
    @CommandCompletion("@duels-custom-banners @dyecolors @duels-banner-patterns")
    @Description("Add a pattern layer to a custom banner cosmetic.")
    fun onBannerPatternAdd(player: ScalaPlayer, @Single id: String, color: DyeColor, @Single patternTypeKey: String)
    {
        val banner = customBanner(id)
        val updatedPatterns = banner.patterns.toMutableList().apply {
            add(DuelsShieldPatternDefinition(color, patternTypeKey.uppercase(Locale.ROOT)))
        }
        replaceCustomBanner(banner.copy(patterns = updatedPatterns))
        player.sendMessage("${CC.GREEN}Added ${CC.WHITE}${color.name}${CC.GREEN} / ${CC.WHITE}${patternTypeKey.uppercase(Locale.ROOT)}${CC.GREEN} to ${CC.YELLOW}${banner.id}${CC.GREEN}.")
    }

    @Subcommand("banner pattern remove")
    @AssignPermission
    @CommandCompletion("@duels-custom-banners")
    @Description("Remove a pattern layer by 1-based index.")
    fun onBannerPatternRemove(player: ScalaPlayer, @Single id: String, index: Int)
    {
        val banner = customBanner(id)
        if (index < 1 || index > banner.patterns.size)
        {
            throw ConditionFailedException("Pattern index must be between 1 and ${banner.patterns.size}.")
        }

        val updatedPatterns = banner.patterns.toMutableList().apply {
            removeAt(index - 1)
        }
        replaceCustomBanner(banner.copy(patterns = updatedPatterns))
        player.sendMessage("${CC.GREEN}Removed pattern ${CC.WHITE}#$index${CC.GREEN} from ${CC.YELLOW}${banner.id}${CC.GREEN}.")
    }

    @Subcommand("banner pattern clear")
    @AssignPermission
    @CommandCompletion("@duels-custom-banners")
    fun onBannerPatternClear(player: ScalaPlayer, @Single id: String)
    {
        val banner = customBanner(id)
        replaceCustomBanner(banner.copy(patterns = mutableListOf()))
        player.sendMessage("${CC.GREEN}Cleared all patterns from ${CC.YELLOW}${banner.id}${CC.GREEN}.")
    }

    @Subcommand("info")
    @Description("Show information about a duels cosmetic.")
    fun onInfo(player: ScalaPlayer, @Single id: String)
    {
        val trim = DuelsCosmeticRegistry.armorTrimById(id)
        if (trim != null)
        {
            player.sendMessage(
                "${CC.GREEN}Duels trim ${CC.YELLOW}${trim.id}${CC.GREEN}:",
                "${CC.GRAY}Display Name: ${CC.WHITE}${trim.displayName}",
                "${CC.GRAY}Cost: ${CC.GOLD}${trim.cost}",
                "${CC.GRAY}Preview Material: ${CC.WHITE}${trim.previewMaterial.name}",
                "${CC.GRAY}Trim Material Key: ${CC.WHITE}${trim.trimMaterialKey}",
                "${CC.GRAY}Trim Pattern Key: ${CC.WHITE}${trim.trimPatternKey}",
                "${CC.GRAY}Built-in: ${CC.WHITE}${DuelsCosmeticRegistry.builtInArmorTrimById(id) != null}",
                "${CC.GRAY}Description: ${CC.WHITE}${trim.description.joinToString(" ${CC.GRAY}| ${CC.WHITE}")}"
            )
            return
        }

        val banner = DuelsCosmeticRegistry.shieldBannerById(id)
            ?: throw ConditionFailedException("No duels cosmetic with the ID ${CC.YELLOW}$id${CC.RED} exists.")

        player.sendMessage(
            "${CC.GREEN}Duels shield banner ${CC.YELLOW}${banner.id}${CC.GREEN}:",
            "${CC.GRAY}Display Name: ${CC.WHITE}${banner.displayName}",
            "${CC.GRAY}Cost: ${CC.GOLD}${banner.cost}",
            "${CC.GRAY}Built-in: ${CC.WHITE}${DuelsCosmeticRegistry.builtInShieldBannerById(id) != null}",
            "${CC.GRAY}Description: ${CC.WHITE}${banner.description.joinToString(" ${CC.GRAY}| ${CC.WHITE}")}",
            "${CC.GRAY}Patterns: ${if (banner.patterns.isEmpty()) "${CC.RED}None" else banner.patterns.mapIndexed { index, pattern -> "${CC.WHITE}${index + 1}. ${pattern.color.name}:${pattern.patternTypeKey}" }.joinToString("${CC.GRAY}, ")}"
        )
    }

    private fun parseDescription(description: String): List<String>
    {
        if (description.equals("none", ignoreCase = true))
        {
            return emptyList()
        }

        return description.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun ensureIdAvailable(id: String)
    {
        if (DuelsCosmeticRegistry.anyCosmeticIdExists(id))
        {
            throw ConditionFailedException("A duels cosmetic with the ID ${CC.YELLOW}$id${CC.RED} already exists.")
        }
    }

    private fun customTrim(id: String): DuelsArmorTrimCosmeticDefinition
    {
        return DuelsCustomCosmeticService.cached().trimById(id)
            ?: throw ConditionFailedException("No custom duels trim with the ID ${CC.YELLOW}$id${CC.RED} exists.")
    }

    private fun customBanner(id: String): DuelsShieldBannerCosmeticDefinition
    {
        return DuelsCustomCosmeticService.cached().shieldBannerById(id)
            ?: throw ConditionFailedException("No custom duels shield banner with the ID ${CC.YELLOW}$id${CC.RED} exists.")
    }

    private fun replaceCustomTrim(updated: DuelsArmorTrimCosmeticDefinition)
    {
        val container = DuelsCustomCosmeticService.cached()
        val index = container.trims.indexOfFirst { it.id.equals(updated.id, true) }
        if (index == -1)
        {
            throw ConditionFailedException("No custom duels trim with the ID ${CC.YELLOW}${updated.id}${CC.RED} exists.")
        }

        container.trims[index] = updated
        DuelsCustomCosmeticService.sync(container)
    }

    private fun replaceCustomBanner(updated: DuelsShieldBannerCosmeticDefinition)
    {
        val container = DuelsCustomCosmeticService.cached()
        val index = container.shieldBanners.indexOfFirst { it.id.equals(updated.id, true) }
        if (index == -1)
        {
            throw ConditionFailedException("No custom duels shield banner with the ID ${CC.YELLOW}${updated.id}${CC.RED} exists.")
        }

        container.shieldBanners[index] = updated
        DuelsCustomCosmeticService.sync(container)
    }
}
