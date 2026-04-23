package gg.tropic.practice.menu.cosmetic

import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Limo
 * @date 2/21/2026
 */
abstract class TemplateCosmeticSearchResultsMenu(
    private val query: String,
    private val page: Int = 0
) : Menu(if (page <= 0) "Cosmetic Search" else "Cosmetic Search (Page ${page + 1})")
{
    companion object
    {
        private val ITEM_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        )

        private const val ITEMS_PER_PAGE = 21
    }

    data class SearchResult(
        val group: TemplateCosmeticGroup,
        val item: TemplateCosmeticItem
    )

    init
    {
        placeholder = true
    }

    abstract fun groups(): List<TemplateCosmeticGroup>
    abstract fun openSearchSign(player: Player, initialQuery: String, callback: (String) -> Unit)
    abstract fun openMainMenu(player: Player)
    abstract fun openSearchResults(player: Player, query: String, page: Int = 0)
    abstract fun openCategoryMenu(player: Player, group: TemplateCosmeticGroup, searchQuery: String)

    protected fun query() = query
    protected fun page() = page

    override fun size(buttons: Map<Int, Button>) = 54

    override fun getButtons(player: Player): MutableMap<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val normalizedQuery = query.trim()

        val results = if (normalizedQuery.isBlank())
        {
            emptyList()
        }
        else
        {
            groups()
                .flatMap { group ->
                    group.entries()
                        .filter {
                            it.displayName.contains(normalizedQuery, ignoreCase = true) ||
                                it.id.contains(normalizedQuery, ignoreCase = true)
                        }
                        .map { item ->
                            SearchResult(group, item)
                        }
                }
                .sortedWith(
                    compareBy<SearchResult> { it.group.displayName.lowercase() }
                        .thenBy { it.item.raritySortOrder }
                        .thenBy { it.item.displayName.lowercase() }
                )
        }

        val totalPages = if (results.isEmpty()) 1 else ((results.size - 1) / ITEMS_PER_PAGE) + 1
        val resolvedPage = page.coerceIn(0, totalPages - 1)
        val pageResults = results
            .drop(resolvedPage * ITEMS_PER_PAGE)
            .take(ITEMS_PER_PAGE)

        pageResults.forEachIndexed { index, result ->
            if (index >= ITEM_SLOTS.size) return@forEachIndexed

            val selectedId = result.group.selectedId(player)
            val selected = selectedId == result.item.id
            val ownedIds = result.group.ownedIds(player)
            val isOwned = ownedIds.contains(result.item.id)

            val builder = if (result.item.skullTexture != null)
            {
                ItemBuilder(ItemStack(Material.SKULL_ITEM, 1, 3.toShort()))
                    .setBase64Head(result.item.skullTexture!!)
            }
            else
            {
                ItemBuilder.of(result.item.material).data(result.item.materialData)
            }

            val nameColor = when
            {
                selected -> CC.GREEN
                isOwned || result.item.isFree() -> CC.YELLOW
                else -> CC.RED
            }

            val statusLore = mutableListOf<String>()
            statusLore += "${CC.GRAY}Category: ${CC.AQUA}${result.group.displayName}"
            statusLore += "${CC.GRAY}Rarity: ${result.item.rarityColorCode}${result.item.rarityDisplayName}"

            if (!result.item.isFree() && !isOwned)
            {
                statusLore += "${CC.GRAY}Cost: ${CC.GOLD}${Numbers.format(result.item.cost)} coins"
            }

            statusLore += ""

            if (selected)
            {
                statusLore += "${CC.GREEN}Currently Selected"
            }
            else if (isOwned || result.item.isFree())
            {
                statusLore += "${CC.AQUA}Unlocked"
            }
            else
            {
                statusLore += "${CC.RED}Locked"
            }

            statusLore += "${CC.YELLOW}Click to open category"

            buttons[ITEM_SLOTS[index]] = builder
                .name("$nameColor${result.item.displayName}")
                .addToLore(
                    *result.item.description.toTypedArray(),
                    "",
                    *statusLore.toTypedArray()
                )
                .toButton { _, _ ->
                    Button.playNeutral(player)
                    openCategoryMenu(player, result.group, normalizedQuery)
                }
        }

        if (pageResults.isEmpty())
        {
            buttons[22] = ItemBuilder
                .of(Material.BARRIER)
                .name("${CC.RED}No matching cosmetics")
                .addToLore(
                    "${CC.GRAY}Search query: ${if (normalizedQuery.isBlank()) "${CC.WHITE}None" else "${CC.WHITE}$normalizedQuery"}",
                    "",
                    "${CC.YELLOW}Click Search Again to try another query!"
                )
                .toButton { _, _ -> }
        }

        if (totalPages > 1)
        {
            buttons[47] = ItemBuilder
                .of(Material.ARROW)
                .name("${CC.YELLOW}Left-click for previous page")
                .addToLore(
                    "${CC.AQUA}Right-click for next page",
                    "",
                    "${CC.GRAY}Page ${resolvedPage + 1}/$totalPages"
                )
                .toButton { _, clickType ->
                    Button.playNeutral(player)

                    val nextPage = if (clickType == ClickType.RIGHT)
                    {
                        (resolvedPage + 1).coerceAtMost(totalPages - 1)
                    }
                    else
                    {
                        (resolvedPage - 1).coerceAtLeast(0)
                    }

                    openSearchResults(player, normalizedQuery, nextPage)
                }
        }

        buttons[48] = ItemBuilder
            .of(Material.COMPASS)
            .name("${CC.GREEN}Search Again")
            .addToLore(
                "${CC.GRAY}Current query: ${if (normalizedQuery.isBlank()) "${CC.WHITE}None" else "${CC.WHITE}$normalizedQuery"}",
                "",
                "${CC.YELLOW}Click to open sign search"
            )
            .toButton { _, _ ->
                Button.playNeutral(player)
                player.closeInventory()

                openSearchSign(player, normalizedQuery) { searchText ->
                    val nextQuery = if (searchText.equals("clear", ignoreCase = true)) "" else searchText
                    val plugin = JavaPlugin.getProvidingPlugin(this::class.java)

                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (!player.isOnline)
                        {
                            return@Runnable
                        }

                        if (nextQuery.isBlank())
                        {
                            openMainMenu(player)
                        }
                        else
                        {
                            openSearchResults(player, nextQuery)
                        }
                    })
                }
            }

        buttons[49] = ItemBuilder
            .of(Material.ARROW)
            .name("${CC.GREEN}Go Back")
            .addToLore("${CC.GRAY}To Cosmetics Menu")
            .toButton { _, _ ->
                Button.playNeutral(player)
                openMainMenu(player)
            }

        buttons[50] = ItemBuilder
            .of(Material.BARRIER)
            .name("${CC.RED}Clear Search")
            .addToLore(
                "${CC.GRAY}Return to the cosmetics categories."
            )
            .toButton { _, _ ->
                Button.playNeutral(player)
                openMainMenu(player)
            }

        return buttons
    }
}
