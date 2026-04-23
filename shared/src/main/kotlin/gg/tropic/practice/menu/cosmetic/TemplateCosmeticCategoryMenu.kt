package gg.tropic.practice.menu.cosmetic

import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Limo
 * @date 2/21/2026
 */
abstract class TemplateCosmeticCategoryMenu(
    private val group: TemplateCosmeticGroup,
    private val page: Int = 0,
    private val searchQuery: String = "",
    private val sortMode: SortMode = SortMode.LOWEST_RARITY_FIRST,
    private val ownedItemsFirst: Boolean = false
) : Menu(if (page <= 0) group.displayName else "${group.displayName} (Page ${page + 1})")
{
    companion object
    {
        val ITEM_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        )
        const val ITEMS_PER_PAGE = 21
    }

    enum class SortMode(val displayName: String)
    {
        LOWEST_RARITY_FIRST("Lowest rarity first"),
        HIGHEST_RARITY_FIRST("Highest rarity first");

        fun next(): SortMode
        {
            return if (this == LOWEST_RARITY_FIRST) HIGHEST_RARITY_FIRST else LOWEST_RARITY_FIRST
        }
    }

    init
    {
        placeholder = true
    }

    protected fun group() = group
    protected fun page() = page
    protected fun searchQuery() = searchQuery
    protected fun sortMode() = sortMode
    protected fun ownedItemsFirst() = ownedItemsFirst

    abstract fun getBalance(player: Player): Long
    abstract fun balanceLabel(): String
    abstract fun openSearchSign(player: Player, initialQuery: String, callback: (String) -> Unit)
    abstract fun openMainMenu(player: Player)

    abstract fun openSelf(
        player: Player,
        newPage: Int = page,
        newSearchQuery: String = searchQuery,
        newSortMode: SortMode = sortMode,
        newOwnedItemsFirst: Boolean = ownedItemsFirst
    )

    open fun canPreview(group: TemplateCosmeticGroup, item: TemplateCosmeticItem): Boolean
    {
        return group.previewable && item.rarityDisplayName != "None"
    }

    open fun previewItem(player: Player, group: TemplateCosmeticGroup, item: TemplateCosmeticItem) {}

    abstract fun attemptPurchase(
        player: Player,
        group: TemplateCosmeticGroup,
        item: TemplateCosmeticItem,
        cost: Long,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    private fun sortEntries(entries: List<TemplateCosmeticItem>, ownedIds: Set<String>): List<TemplateCosmeticItem>
    {
        val comparator = when (sortMode)
        {
            SortMode.LOWEST_RARITY_FIRST -> compareBy<TemplateCosmeticItem> { if (ownedItemsFirst && !ownedIds.contains(it.id)) 1 else 0 }
                .thenByDescending { it.raritySortOrder }
                .thenBy { it.displayName.lowercase() }

            SortMode.HIGHEST_RARITY_FIRST -> compareBy<TemplateCosmeticItem> { if (ownedItemsFirst && !ownedIds.contains(it.id)) 1 else 0 }
                .thenBy { it.raritySortOrder }
                .thenBy { it.displayName.lowercase() }
        }

        return entries.sortedWith(comparator)
    }

    override fun size(buttons: Map<Int, Button>) = 54

    override fun getButtons(player: Player): MutableMap<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val selectedId = group.selectedId(player)
        val ownedIds = group.ownedIds(player)

        val normalizedQuery = searchQuery.trim()
        val matchingEntries = group.entries().filter {
            normalizedQuery.isBlank() ||
                it.displayName.contains(normalizedQuery, ignoreCase = true) ||
                it.id.contains(normalizedQuery, ignoreCase = true)
        }

        val sortedEntries = sortEntries(matchingEntries, ownedIds)
        val totalPages = if (sortedEntries.isEmpty()) 1 else ((sortedEntries.size - 1) / ITEMS_PER_PAGE) + 1
        val resolvedPage = page.coerceIn(0, totalPages - 1)
        val pageEntries = sortedEntries
            .drop(resolvedPage * ITEMS_PER_PAGE)
            .take(ITEMS_PER_PAGE)

        pageEntries.forEachIndexed { index, entry ->
            if (index >= ITEM_SLOTS.size) return@forEachIndexed

            val slot = ITEM_SLOTS[index]
            val isSelected = entry.id == selectedId
            val isOwned = ownedIds.contains(entry.id)
            buttons[slot] = createCosmeticButton(player, entry, isSelected, isOwned, resolvedPage)
        }

        if (pageEntries.isEmpty())
        {
            buttons[22] = ItemBuilder.of(Material.BARRIER)
                .name("${CC.RED}No matching cosmetics")
                .addToLore(
                    "${CC.GRAY}Search: ${if (normalizedQuery.isBlank()) "${CC.WHITE}None" else "${CC.WHITE}$normalizedQuery"}",
                    "",
                    "${CC.YELLOW}Click the search button to try again!"
                )
                .toButton { _, _ -> }
        }

        if (totalPages > 1)
        {
            val leftClickLabel: String
            val rightClickLabel: String

            if (resolvedPage == 0)
            {
                leftClickLabel = "Left-click for next page!"
                rightClickLabel = "Right-click for last page!"
            }
            else
            {
                leftClickLabel = "Left-click for previous page!"
                rightClickLabel = "Right-click for first page!"
            }

            buttons[47] = ItemBuilder.of(Material.ARROW)
                .name("${CC.YELLOW}$leftClickLabel")
                .addToLore(
                    "${CC.AQUA}$rightClickLabel",
                    "",
                    "${CC.GRAY}Page ${resolvedPage + 1}/$totalPages"
                )
                .toButton { _, clickType ->
                    Button.playNeutral(player)

                    val nextPage = if (clickType == ClickType.RIGHT)
                    {
                        if (resolvedPage == 0) totalPages - 1 else 0
                    }
                    else
                    {
                        if (resolvedPage == 0) resolvedPage + 1 else resolvedPage - 1
                    }

                    openSelf(player, nextPage)
                }
        }

        val searchLore = mutableListOf<String>()
        if (normalizedQuery.isBlank())
        {
            searchLore += "${CC.GRAY}Use this feature to easily find a"
            searchLore += "${CC.GRAY}specific cosmetic item."
        }
        else
        {
            searchLore += "${CC.GRAY}Search query: ${CC.WHITE}$normalizedQuery"
            searchLore += "${CC.GRAY}Results: ${CC.GREEN}${matchingEntries.size}"
            searchLore += ""
            searchLore += "${CC.YELLOW}Left-click to search again!"
            searchLore += "${CC.YELLOW}Right-click to clear search!"
        }

        buttons[48] = ItemBuilder.of(Material.COMPASS)
            .name("${CC.GREEN}Search")
            .addToLore(*searchLore.toTypedArray())
            .toButton { _, clickType ->
                if (clickType == ClickType.RIGHT && normalizedQuery.isNotBlank())
                {
                    Button.playNeutral(player)
                    openSelf(player, 0, "", sortMode, ownedItemsFirst)
                    return@toButton
                }

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

                        openSelf(player, 0, nextQuery, sortMode, ownedItemsFirst)
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

        buttons[50] = ItemBuilder.of(Material.HOPPER)
            .name("${CC.GREEN}Sorted by: ${CC.WHITE}${sortMode.displayName}")
            .addToLore(
                "${CC.GRAY}Sorts by rarity: ${CC.WHITE}${sortMode.displayName}",
                "",
                "${CC.GRAY}Next sort: ${CC.GREEN}${sortMode.next().displayName}",
                "${CC.YELLOW}Left click to use!",
                "",
                "${CC.GRAY}Owned items first: ${if (ownedItemsFirst) "${CC.GREEN}Yes" else "${CC.RED}No"}",
                "${CC.YELLOW}Right click to toggle!"
            )
            .toButton { _, clickType ->
                Button.playNeutral(player)

                if (clickType == ClickType.RIGHT)
                {
                    openSelf(player, 0, searchQuery, sortMode, !ownedItemsFirst)
                }
                else
                {
                    openSelf(player, 0, searchQuery, sortMode.next(), ownedItemsFirst)
                }
            }

        buttons[51] = run {
            val balance = getBalance(player)
            ItemBuilder
                .of(Material.EMERALD)
                .name("${CC.GREEN}Balance")
                .addToLore(
                    "",
                    "${CC.GRAY}Your ${balanceLabel()}: ${CC.GOLD}${Numbers.format(balance)}",
                    ""
                )
                .toButton { _, _ -> }
        }

        return buttons
    }

    private fun createCosmeticButton(
        player: Player,
        entry: TemplateCosmeticItem,
        isSelected: Boolean,
        isOwned: Boolean,
        activePage: Int
    ): Button
    {
        val nameColor = when
        {
            isSelected -> CC.GREEN
            isOwned || entry.isFree() -> CC.YELLOW
            else -> CC.RED
        }
        val lore = entry.buildLore(group.displayName, isSelected, isOwned, group.previewable)

        val builder = if (entry.skullTexture != null)
        {
            ItemBuilder(ItemStack(Material.SKULL_ITEM, 1, 3.toShort()))
                .setBase64Head(entry.skullTexture!!)
                .name("$nameColor${entry.displayName}")
                .addToLore(*lore.toTypedArray())
        }
        else
        {
            ItemBuilder
                .copyOf(entry.displayItem())
                .name("$nameColor${entry.displayName}")
                .addToLore(*lore.toTypedArray())
        }

        if (isSelected)
        {
            builder
                .enchant(Enchantment.DURABILITY, 1)
                .addFlags(ItemFlag.HIDE_ENCHANTS)
        }

        builder.addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS)

        return builder.toButton { _, clickType ->
            if (clickType == ClickType.RIGHT && canPreview(group, entry))
            {
                previewItem(player, group, entry)
                return@toButton
            }

            if (isSelected)
            {
                group.select(player, "none")
                group.saveProfile(player)

                Button.playNeutral(player)
                player.sendMessage(
                    "${CC.YELLOW}Deselected ${CC.WHITE}${entry.displayName}${CC.YELLOW} from ${group.displayName}."
                )

                openSelf(player, activePage)
                return@toButton
            }

            val actuallyOwned = group.ownedIds(player).contains(entry.id)

            if (actuallyOwned || entry.isFree())
            {
                group.select(player, entry.id)
                group.saveProfile(player)

                Button.playNeutral(player)
                player.sendMessage(
                    "${CC.GREEN}Selected ${CC.WHITE}${entry.displayName}${CC.GREEN} as your ${group.displayName}!"
                )

                openSelf(player, activePage)
                return@toButton
            }

            val cost = entry.cost

            attemptPurchase(player, group, entry, cost,
                onSuccess = {
                    group.unlock(player, entry.id)
                    group.select(player, entry.id)
                    group.saveProfile(player)

                    Button.playNeutral(player)
                    player.sendMessage(
                        "${CC.GREEN}Purchased and selected ${CC.WHITE}${entry.displayName}${CC.GREEN} " +
                        "for ${CC.GOLD}${Numbers.format(cost)} coins${CC.GREEN}!"
                    )

                    openSelf(player, activePage)
                },
                onFailure = { message ->
                    player.sendMessage(message)
                    Button.playFail(player)
                }
            )
        }
    }
}
