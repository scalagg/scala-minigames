package gg.tropic.practice.commands.customizers

import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.tropic.practice.cosmetic.DuelsCosmeticRegistry
import gg.tropic.practice.cosmetic.DuelsCustomCosmeticService
import org.bukkit.DyeColor

object DuelsCosmeticCommandCustomizers
{
    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager)
    {
        manager.commandCompletions.registerAsyncCompletion("duels-custom-trims") {
            runCatching { DuelsCustomCosmeticService.cached().trims.map { trim -> trim.id } }.getOrDefault(emptyList())
        }

        manager.commandCompletions.registerAsyncCompletion("duels-custom-banners") {
            runCatching { DuelsCustomCosmeticService.cached().shieldBanners.map { banner -> banner.id } }.getOrDefault(emptyList())
        }

        manager.commandCompletions.registerAsyncCompletion("duels-trim-materials") {
            DuelsCosmeticRegistry.armorTrimMaterialKeys
        }

        manager.commandCompletions.registerAsyncCompletion("duels-trim-patterns") {
            DuelsCosmeticRegistry.armorTrimPatternKeys
        }

        manager.commandCompletions.registerAsyncCompletion("duels-banner-patterns") {
            DuelsCosmeticRegistry.shieldBannerPatternKeys
        }

        manager.commandCompletions.registerAsyncCompletion("dyecolors") {
            DyeColor.entries.map { it.name }
        }
    }
}
