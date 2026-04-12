package gg.tropic.practice.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.commons.playerstatus.isVirtuallyInvisibleToSomeExtent
import gg.scala.staff.ScalaStaffPlugin
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.kit.category.KitCategoryService
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.leaderboards.CommonlyViewedLeaderboardType
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.queue.queueId
import gg.tropic.practice.metadata.SystemMetadataService
import gg.tropic.practice.services.LeaderboardManagerService
import gg.tropic.practice.statistics.StatisticID
import gg.tropic.practice.utilities.MenuConstants
import gg.tropic.practice.versioned.MinigameModernAccess
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemFlag

class JoinQueueMenu(
    player: Player,
    private val queueType: QueueType,
    private val teamSize: Int
) : TemplateKitMenu(player, dynamic = true)
{
    init
    {
        async = false
        autoUpdate = true
    }

    override fun getItemAmount(player: Player, kit: Kit) = SystemMetadataService
        .getPlaying(queueId {
            kit(kit)
            queueType(queueType)
            teamSize(teamSize)
        })
        .coerceIn(1..64)

    override fun getAutoUpdateTicks() = 500L

    override fun size(buttons: Map<Int, Button>) = 54

    override fun getMaxItemsPerPage(player: Player) = 14

    override fun getAllPagesButtonSlots() = MenuConstants.SLOTS_TRIPLE.take(14).map { it + 9 }

    override fun filterDisplayOfKit(player: Player, kit: Kit) = kit.queueSizes
        .any {
            it.first == teamSize && queueType in it.second
        }

    override fun getGlobalButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val inactivePane = XMaterial.GRAY_STAINED_GLASS_PANE.parseMaterial() ?: Material.STAINED_GLASS_PANE
        val activePane = XMaterial.LIME_STAINED_GLASS_PANE.parseMaterial() ?: Material.STAINED_GLASS_PANE
        val noFavoriteKitIcon = XMaterial.RED_STAINED_GLASS_PANE.parseMaterial() ?: Material.STAINED_GLASS_PANE

        for (slot in 9..17)
        {
            buttons[slot] = Button.placeholder(ItemBuilder(inactivePane).name(" ").build())
        }

        var i = 1
        i++

        if (category == null)
        {
            buttons[i + 9] = ItemBuilder(activePane)
                .name(" ")
                .toButton()
        }

        val favoriteSlots = listOf(47, 48, 49, 50, 51)

        favoriteSlots.forEachIndexed { index, slot ->
            val favoriteKit = PracticeProfileService.find(player)?.favoriteKits?.getOrNull(index)

            val kit = KitService.cached().kits[favoriteKit]
            buttons[slot] = ItemBuilder
                .copyOf(
                    kit?.displayIcon ?: ItemBuilder.of(noFavoriteKitIcon).build()
                )
                .data(
                    kit?.displayIcon?.data?.data?.toShort() ?: 14.toShort()
                )
                .name(
                    if (kit == null)
                    {
                        "${CC.B_RED}Empty Slot"
                    } else
                    {
                        "${CC.PRI}${kit.displayName}"
                    }
                )
                .addFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_POTION_EFFECTS
                )
                .addToLore(
                    *favoritesItemDescriptionOf(player, kit)
                ).toButton { _, type ->
                    if (kit == null)
                    {
                        FavoriteKitsMenu(player, queueType, teamSize).openMenu(player)
                        return@toButton
                    }

                    if (type == ClickType.LEFT)
                    {
                        itemClicked(player, kit, type)
                        return@toButton
                    }

                    if (type == ClickType.RIGHT)
                    {
                        PracticeProfileService.find(player)?.favoriteKits?.remove(kit.id)
                        player.sendMessage("${CC.RED}You have removed ${kit.displayName} from your favorite kits.")
                        openMenu(player)
                        return@toButton
                    }

                    if (type == ClickType.MIDDLE)
                    {
                        FavoriteKitsMenu(player, queueType, teamSize).openMenu(player)
                    }
                }
        }

        buttons[i] = ItemBuilder(Material.EMERALD)
            .name("${CC.YELLOW}All Kits")
            .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS)
            .addToLore(
                "${CC.GRAY}Browse every available duel kit.",
                "",
                "${CC.GREEN}Click to reset the filter."
            )
            .toButton { _, _ ->
                if (category == null)
                {
                    return@toButton
                }
                category = null
                openMenu(player)
                MinigameModernAccess.playMenuSound(player)
            }

        KitCategoryService.cached()
            .categories
            .forEach { kitCategory ->
                i++

                if (category == kitCategory)
                {
                    buttons[i + 9] = ItemBuilder(activePane)
                        .name(" ")
                        .toButton()
                }

                var playing = 0
                var waiting = 0
                KitService.cached().kits.values
                    .filter { kit -> kitCategory.kits.contains(kit.id) }
                    .forEach { kit ->
                        val queueId = "${kit.id}:${queueType.name}:${teamSize}v${teamSize}"
                        playing += SystemMetadataService.getPlaying(queueId)
                        waiting += SystemMetadataService.getQueued(queueId)
                    }

                buttons[i] = ItemBuilder(kitCategory.item)
                    .name("${CC.YELLOW}${kitCategory.name}${if (kitCategory.newCategory) " ${CC.B_AQUA}NEW!" else ""}")
                    .addFlags(
                        ItemFlag.HIDE_ATTRIBUTES,
                        ItemFlag.HIDE_ENCHANTS,
                        ItemFlag.HIDE_POTION_EFFECTS
                    )
                    .setLore(kitCategory.description.map { "${CC.GRAY}$it" })
                    .addToLore(
                        "",
                        "${CC.WHITE}Playing: ${CC.PRI}$playing",
                        "${CC.WHITE}Waiting: ${CC.PRI}$waiting",
                        "",
                        "${CC.GREEN}Click to filter this category."
                    )
                    .toButton { _, _ ->
                        if (category == kitCategory)
                        {
                            return@toButton
                        }

                        category = kitCategory
                        openMenu(player)
                        MinigameModernAccess.playMenuSound(player)
                    }

            }

        return buttons
    }

    override fun itemTitleFor(player: Player, kit: Kit) = "${CC.PRI}${kit.displayName}${
        if (kit.features(FeatureFlag.NewlyCreated)) " ${CC.B_AQUA}NEW!" else ""
    }"

    override fun itemDescriptionOf(player: Player, kit: Kit): List<String>
    {
        val queueId = "${kit.id}:${queueType.name}:${teamSize}v${teamSize}"
        val playing = SystemMetadataService.getPlaying(queueId)
        val queued = SystemMetadataService.getQueued(queueId)

        return listOf(
            "${CC.GRAY}Playing: ${CC.PRI}$playing",
            "${CC.GRAY}Queuing: ${CC.PRI}$queued",
            "",
            *topLeaderboardConciseDescription(
                player,
                (if (queueType == QueueType.Ranked) CommonlyViewedLeaderboardType.ELO else CommonlyViewedLeaderboardType.CasualWinStreak)
                    .toStatisticID(kit),
                if (queueType == QueueType.Ranked) "ELO" else "Daily Streak"
            ).toTypedArray(),
            "",
            "${CC.GREEN}Click to queue!"
        )
    }

    private fun favoritesItemDescriptionOf(player: Player, kit: Kit?): Array<String>
    {
        return if (kit == null)
        {
            arrayOf("${CC.GRAY}Click to add a favorite kit.")
        } else
        {
            val queueId = "${kit.id}:${queueType.name}:${teamSize}v${teamSize}"
            val playing = SystemMetadataService.getPlaying(queueId)
            val queued = SystemMetadataService.getQueued(queueId)
            val isRanked = queueType == QueueType.Ranked

            val kitLore = mutableListOf<String>()

            if (isRanked && !kit.features.contains(FeatureFlag.Ranked))
            {
                kitLore.add("")
                kitLore.add("${CC.RED}This kit does not support ranked.")
            } else
            {
                kitLore.add("${CC.GRAY}Playing: ${CC.PRI}$playing")
                kitLore.add("${CC.GRAY}Waiting: ${CC.PRI}$queued")
                kitLore.add("")
                kitLore.addAll(
                    topLeaderboardConciseDescription(
                        player,
                        (if (queueType == QueueType.Ranked) CommonlyViewedLeaderboardType.ELO else CommonlyViewedLeaderboardType.CasualWinStreak)
                            .toStatisticID(kit),
                        if (queueType == QueueType.Ranked) "ELO" else "Daily Streak"
                    )
                )
            }

            kitLore.add("")
            kitLore.add("${CC.GREEN}Left-Click to queue")
            kitLore.add("${CC.YELLOW}Middle-Click to modify")
            kitLore.add("${CC.RED}Right-Click to remove")
            kitLore.toTypedArray()
        }
    }

    private fun topLeaderboardConciseDescription(
        player: Player,
        leaderboardID: StatisticID,
        label: String
    ): List<String>
    {
        val statistic = PracticeProfileService.find(player)
            ?.getStatisticValue(leaderboardID)
            ?: return LeaderboardManagerService
                .getCachedFormattedLeaderboards(leaderboardID.toId())
                .take(3)

        val personalScore = listOf(
            "${CC.PRI}Your $label: ${CC.WHITE}${
                Numbers.format(statistic.score.toLong())
            } ${
                if (statistic.value != -1L) "${CC.GRAY}(#${
                    Numbers.format(statistic.value + 1)
                })" else ""
            }"
        )

        return personalScore + LeaderboardManagerService
            .getCachedFormattedLeaderboards(leaderboardID.toId())
            .take(3)
    }

    override fun itemClicked(player: Player, kit: Kit, type: ClickType)
    {
        val lobbyPlayer = LobbyPlayerService
            .find(player.uniqueId)
            ?: return

        if (lobbyPlayer.state != PlayerState.Idle)
        {
            player.sendMessage("${CC.RED}You cannot join a queue right now!")
            return
        }

        if (player.isVirtuallyInvisibleToSomeExtent())
        {
            player.sendMessage("${CC.RED}You are currently in vanish! Use ${CC.B}/vanish${CC.RED} to be able to join a queue.")
            return
        }

        val basicsProfile = BasicsProfileService.find(player)
        if (basicsProfile != null && player.hasPermission(ScalaStaffPlugin.STAFF_NODE))
        {
            if (basicsProfile.setting("auto-vanish", StateSettingValue.DISABLED) == StateSettingValue.ENABLED)
            {
                player.sendMessage("${CC.RED}You currently have AutoVanish enabled! Use ${CC.B}/toggleautovanish${CC.RED} to be able to join a queue.")
                return
            }
        }

        player.closeInventory()
        QueueService.joinQueue(kit, queueType, teamSize, player)

        Button.playNeutral(player)
        player.sendMessage(
            "${CC.GREEN}You have joined the ${CC.D_GREEN}${queueType.name} ${teamSize}v$teamSize ${kit.displayName}${CC.GREEN} queue!"
        )
    }

    override fun getPrePaginatedTitle(player: Player) =
        "Queueing ${queueType.name} ${teamSize}v$teamSize..."
}
