package gg.tropic.practice.menu.cosmetic.duels

import gg.scala.commons.sign.RaphaSignGUI
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object DuelsCosmeticSignInput
{
    private val signGUI by lazy {
        RaphaSignGUI(JavaPlugin.getProvidingPlugin(this::class.java))
    }

    fun openSearch(player: Player, initialQuery: String = "", action: (String) -> Unit)
    {
        signGUI.open(player, arrayOf(initialQuery.take(15), "^^^^^^^^^^^^^^", "Search duel", "cosmetics")) { _, resultLines ->
            action((resultLines.firstOrNull() ?: "").trim().replace("""\s+""".toRegex(), " ").take(32))
        }
    }
}
