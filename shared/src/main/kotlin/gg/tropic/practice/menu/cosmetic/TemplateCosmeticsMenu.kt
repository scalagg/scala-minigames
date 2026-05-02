package gg.tropic.practice.menu.cosmetic

import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Limo
 * @date 2/21/2026
 */
abstract class TemplateCosmeticsMenu : Menu("My Cosmetics")
{
    init
    {
        placeholder = true
    }

    abstract fun groups(): List<TemplateCosmeticGroup>
    abstract fun getBalance(player: Player): Long
    abstract fun balanceLabel(): String
    abstract fun backLabel(): String

    open fun onBackClick(player: Player)
    {
        player.closeInventory()
    }

    abstract fun openSearchSign(player: Player, callback: (String) -> Unit)
    abstract fun openSearchResults(player: Player, query: String)
    abstract fun openSelf(player: Player)

    override fun size(buttons: Map<Int, Button>) = 54

    override fun getButtons(player: Player): MutableMap<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        groups().forEach { group ->
            val entries = group.entries()
            val selectedId = group.selectedId(player)
            val selectedEntry = entries.firstOrNull { it.id == selectedId }
            val selectedDisplay = if (selectedId.equals("none", ignoreCase = true))
                "NONE" else (selectedEntry?.displayName ?: "None")
            val unlockedCount = group.ownedCount(player)
            val totalCount = entries.size
            val unlockedPercent = if (totalCount == 0) 0 else ((unlockedCount * 100) / totalCount)

            val builder = if (group.skullTexture != null)
            {
                ItemBuilder(ItemStack(Material.PLAYER_HEAD, 1))
                    .setBase64Head(group.skullTexture!!)
            }
            else
            {
                ItemBuilder.of(group.iconMaterial).data(group.iconData)
            }

            buttons[group.menuSlot] = builder
                .name("${CC.GREEN}${group.displayName}")
                .addToLore(
                    *group.description.toTypedArray(),
                    "",
                    "${CC.GRAY}Unlocked: ${CC.GREEN}${Numbers.format(unlockedCount.toLong())}/${Numbers.format(totalCount.toLong())} ${CC.GRAY}(${unlockedPercent}%)",
                    "${CC.GRAY}Currently Selected:",
                    "${CC.GREEN}$selectedDisplay",
                    "",
                    "${CC.YELLOW}Click to view!"
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)
                    onGroupClick(player, group)
                }
        }

        val balance = getBalance(player)
        buttons[48] = ItemBuilder
            .of(Material.EMERALD)
            .name("${CC.GREEN}Balance")
            .addToLore(
                "",
                "${CC.GRAY}Your ${balanceLabel()}: ${CC.GOLD}${Numbers.format(balance)}",
                ""
            )
            .toButton { _, _ -> }

        buttons[49] = ItemBuilder
            .of(Material.ARROW)
            .name("${CC.GREEN}Go Back")
            .addToLore("${CC.GRAY}${backLabel()}")
            .toButton { _, _ ->
                Button.playNeutral(player)
                onBackClick(player)
            }

        buttons[50] = ItemBuilder
            .of(Material.COMPASS)
            .name("${CC.GREEN}Search Cosmetics")
            .addToLore(
                "${CC.GRAY}Search across all cosmetic categories",
                "${CC.GRAY}without opening each category first.",
                "",
                "${CC.YELLOW}Click to open sign search!"
            )
            .toButton { _, _ ->
                Button.playNeutral(player)
                player.closeInventory()

                openSearchSign(player) { searchText ->
                    val nextQuery = if (searchText.equals("clear", ignoreCase = true)) "" else searchText
                    val plugin = JavaPlugin.getProvidingPlugin(this::class.java)

                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (!player.isOnline)
                        {
                            return@Runnable
                        }

                        if (nextQuery.isBlank())
                        {
                            openSelf(player)
                        }
                        else
                        {
                            openSearchResults(player, nextQuery)
                        }
                    })
                }
            }

        return buttons
    }

    abstract fun onGroupClick(player: Player, group: TemplateCosmeticGroup)
}
