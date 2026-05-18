package gg.tropic.practice.player.hotbar

import com.cryptomorin.xseries.XMaterial
import gg.scala.basics.plugin.settings.SettingMenu
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.commons.metadata.SpigotNetworkMetadataDataSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.hotbar.HotbarPreset
import gg.scala.lemon.hotbar.HotbarPresetHandler
import gg.scala.lemon.hotbar.entry.impl.DynamicHotbarPresetEntry
import gg.scala.lemon.hotbar.entry.impl.StaticHotbarPresetEntry
import gg.scala.lemon.redirection.expectation.PlayerJoinWithExpectationEvent
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.queue.spigot.command.LeaveQueueCommand
import gg.tropic.practice.Globals
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.configuration.PracticeConfigurationService
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.menu.JoinQueueMenu
import gg.tropic.practice.menu.PlayerMainMenu
import gg.tropic.practice.menu.cosmetic.duels.DuelsCosmeticsMenu
import gg.tropic.practice.menu.editor.EditorKitSelectionMenu
import gg.tropic.practice.menu.party.PartyPlayGameSelectMenu

import gg.tropic.practice.minigame.MinigameLobby
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.statistics.TrackedKitStatistic
import gg.tropic.practice.statistics.statisticIdFrom
import gg.tropic.practice.versioned.MinigameModernAccess
import mc.arch.minigames.parties.menu.PartyManageMenu
import mc.arch.minigames.parties.service.NetworkPartyService
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu

import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
object LobbyHotbarService
{
    @Inject
    lateinit var plugin: PracticeLobby

    @Inject
    lateinit var audiences: BukkitAudiences

    var buildNewBotFightMenu: (Player) -> Menu? = { null }
    var buildBotSelectorMenu: (Player) -> Menu? = { null }

    private val hotbarCache = mutableMapOf<PlayerState, HotbarPreset>()

    private fun modernName(label: String, first: String, second: String, legacy: String): String
    {
        return if (MinigameModernAccess.isModern()) {
            MinigameModernAccess.gradient(label, first, second)
        } else {
            legacy
        }
    }

    private fun hotbarItem(material: Material, title: String, lore: Array<out String> = emptyArray()): ItemBuilder
    {
        return ItemBuilder(material)
            .name(title)
            .addFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP
            )
            .apply {
                if (lore.isNotEmpty()) {
                    addToLore(*lore)
                }
            }
    }

    fun openRankedQueueMenu(player: Player)
    {
        val profile = PracticeProfileService.find(player)
            ?: return

        val configuration = PracticeConfigurationService.cached()
        if (!player.isOp && !configuration.rankedQueueEnabled)
        {
            player.sendMessage("${CC.RED}Ranked queues are temporarily disabled. Please try again later.")
            return
        }

        if (player.hasMetadata("frozen"))
        {
            player.sendMessage("${CC.RED}You cannot join queues while frozen!")
            return
        }

        Schedulers
            .async()
            .run {
                val globalWins = profile
                    .getStatisticValue(
                        statisticIdFrom(TrackedKitStatistic.Wins)
                    )
                    ?.score
                    ?.toLong()
                    ?: 0

                if (
                    !player.hasPermission("practice.bypass-ranked-queue-requirements")
                    && globalWins < 10
                )
                {
                    player.sendMessage(
                        "${CC.RED}You must have at least 10 wins to queue for a Ranked kit! You currently have ${CC.YELLOW}${
                            if (globalWins == 0L) "no wins" else "$globalWins win${
                                if (globalWins == 1L) "" else "s"
                            }"
                        }${CC.RED}."
                    )
                    return@run
                }

                if (profile.hasActiveRankedBan())
                {
                    profile.deliverRankedBanMessage(player)
                    return@run
                }

                JoinQueueMenu(player, QueueType.Ranked, 1).openMenu(player)
            }
    }

    @Configure
    fun configure()
    {
        val idlePreset = HotbarPreset()

        val wasGameParticipant = mutableSetOf<UUID>()
        val loginTasks = mutableMapOf<UUID, MutableList<(Player) -> Unit>>()

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                wasGameParticipant.remove(it.player.uniqueId)
                loginTasks.remove(it.player.uniqueId)
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerJoinEvent::class.java, EventPriority.MONITOR)
            .handler {
                if (it.player.uniqueId !in wasGameParticipant)
                {
                    with(PracticeConfigurationService.local()) {
                        if (loginMOTD.isNotEmpty())
                        {
                            loginMOTD.forEach(it.player::sendMessage)
                        }
                    }
                }

                Tasks.delayed(5L) {
                    loginTasks[it.player.uniqueId]
                        ?.forEach { task ->
                            task.invoke(it.player)
                        }
                }
            }

        Events
            .subscribe(PlayerJoinWithExpectationEvent::class.java)
            .handler {
                if (it.response.parameters.containsKey("was-game-participant"))
                {
                    wasGameParticipant += it.uniqueId
                }

                if (it.response.parameters.containsKey("requeue-kit-id"))
                {
                    val rematchKitID = it.response.parameters["requeue-kit-id"]
                    val rematchQueueType = QueueType.valueOf(
                        it.response.parameters["requeue-queue-type"]!!
                    )

                    val kit = KitService.cached().kits[rematchKitID]
                        ?: return@handler

                    loginTasks.getOrPut(it.uniqueId, ::mutableListOf) += { player ->
                        QueueService.joinQueue(
                            kit = kit,
                            queueType = rematchQueueType,
                            teamSize = 1,
                            player = player
                        )
                    }
                }

                if (it.response.parameters.containsKey("rematch-kit-id"))
                {
                    val rematchKitID = it.response.parameters["rematch-kit-id"]
                    val rematchTargetID = it.response.parameters["rematch-target-id"]

                    val kit = KitService.cached().kits[rematchKitID]
                        ?: return@handler

                    val rematchUuid = UUID.fromString(rematchTargetID)

                    if (rematchUuid in Globals.POSSIBLE_PLAYER_BOT_UNIQUE_IDS)
                    {
                        return@handler
                    }

                    loginTasks.getOrPut(it.uniqueId, ::mutableListOf) += { player ->
                        val username = rematchUuid.username()
                        val rematchItem = ItemBuilder
                            .of(Material.PAPER)
                            .name(
                                "${CC.D_GREEN}Rematch $username ${CC.GRAY}(Right Click)"
                            )
                            .build()

                        val terminable = CompositeTerminable.create()
                        Events
                            .subscribe(PlayerInteractEvent::class.java)
                            .filter { event ->
                                event.hasItem() &&
                                    event.action.name.contains("RIGHT") &&
                                    event.item!!.isSimilar(rematchItem)
                            }
                            .handler {
                                player.chat("/duel $username ${kit.id}")

                                Button.playNeutral(player)
                                terminable.closeAndReportException()
                            }
                            .bindWith(terminable)

                        Schedulers
                            .sync()
                            .runLater({
                                terminable.closeAndReportException()
                            }, 20L * 30L)
                            .bindWith(terminable)

                        Events
                            .subscribe(PlayerQuitEvent::class.java)
                            .filter { event ->
                                event.player.uniqueId == player.uniqueId
                            }
                            .handler {
                                terminable.closeAndReportException()
                            }
                            .bindWith(terminable)

                        terminable.with {
                            player.inventory.setItem(3, ItemStack(Material.AIR))
                        }

                        player.inventory.setItem(3, rematchItem)
                    }
                }
            }
            .bindWith(plugin)

        val createParty = DynamicHotbarPresetEntry().apply {
            onBuild = context@{ player ->
                if (MinigameLobby.isMinigameLobby() || MinigameLobby.isMainLobby())
                {
                    return@context ItemStack(Material.AIR)
                }

                val built = hotbarItem(
                    material = MinigameModernAccess.duelsCreatePartyMaterial(),
                    title = modernName("Create Party", "#f7c64a", "#ffdca8", "${CC.GOLD}Create Party"),
                    lore = arrayOf(
                        "${CC.GRAY}Create a party with your friends",
                        "${CC.GRAY}to queue and play together.",
                        "",
                        "${CC.GREEN}Right-click to create a party."
                    )
                )
                    .build()

                return@context MinigameModernAccess.applySingGoatHorn(built)
            }

            onClick = context@{ player ->
                MinigameModernAccess.playCreatePartySound(player)

                CompletableFuture.supplyAsync<Boolean> {
                    if (NetworkPartyService.findParty(player.uniqueId) != null)
                    {
                        return@supplyAsync false
                    }

                    NetworkPartyService.createParty(player.uniqueId)
                    true
                }.thenAccept {
                    Tasks.sync {
                        if (NetworkPartyService.findParty(player.uniqueId) == null)
                        {
                            player.sendMessage("${CC.RED}Your party could not be created right now.")
                            return@sync
                        }

                        player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${" ".repeat(53)}")
                        player.sendMessage("${CC.GREEN}You have created a new party!")
                        player.sendMessage("${CC.YELLOW}Use ${CC.AQUA}/party help${CC.YELLOW} to view all party-related commands!")
                        player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${" ".repeat(53)}")

                        val lobbyPlayer = LobbyPlayerService.find(player)
                            ?: return@sync

                        lobbyPlayer.maintainStateTimeout = -1L
                        lobbyPlayer.syncQueueState()

                        Tasks.delayed(2L) {
                            LobbyPlayerService.find(player)?.syncQueueState()
                        }
                    }
                }
            }
        }

        val cosmetics = StaticHotbarPresetEntry(
            hotbarItem(
                material = MinigameModernAccess.duelsCosmeticsMaterial(),
                title = modernName("Cosmetics", "#d38dff", "#ffd8f4", "${CC.GREEN}Cosmetics"),
                lore = arrayOf(
                    "${CC.GRAY}Customize your lobby and match",
                    "${CC.GRAY}appearance with unlockable styles.",
                    "",
                    "${CC.GREEN}Right-click to browse cosmetics."
                )
            )
        ).also {
            it.onClick = { player ->
                DuelsCosmeticsMenu().openMenu(player)
                MinigameModernAccess.playMenuSound(player)
            }
        }

        val settings = StaticHotbarPresetEntry(
            hotbarItem(
                material = MinigameModernAccess.duelsSettingsMaterial(),
                title = modernName("Settings", "#f7d35f", "#fff2b0", "${CC.D_PURPLE}Settings"),
                lore = arrayOf(
                    "${CC.GRAY}Manage your preferences,",
                    "${CC.GRAY}scoreboard, and duel settings.",
                    "",
                    "${CC.GREEN}Right-click to open settings."
                )
            )
        ).also {
            it.onClick = { player ->
                SettingMenu(player).openMenu(player)
                MinigameModernAccess.playMenuSound(player)
            }
        }

        idlePreset.addSlot(
            7,
            StaticHotbarPresetEntry(
                hotbarItem(
                    material = MinigameModernAccess.duelsNavigatorMaterial(),
                    title = modernName("Navigator", "#63d8ff", "#d2fbff", "${CC.D_PURPLE}Navigator"),
                    lore = arrayOf(
                        "${CC.GRAY}Browse featured menus, kit editing,",
                        "${CC.GRAY}stats, events, and parkour.",
                        "",
                        "${CC.GREEN}Right-click to open the navigator."
                    )
                )
            ).also {
                it.onClick = { player ->
                    MinigameModernAccess.playMenuSound(player)
                    PlayerMainMenu().openMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            0,
            StaticHotbarPresetEntry(
                hotbarItem(
                    material = MinigameModernAccess.duelsCasualQueueMaterial(),
                    title = modernName("Casual Queue", "#6cf18d", "#d6ff9f", "${CC.GREEN}Play Casual"),
                    lore = arrayOf(
                        "${CC.GRAY}Jump into relaxed unranked fights",
                        "${CC.GRAY}with fast matchmaking.",
                        "",
                        "${CC.GREEN}Right-click to select a queue."
                    )
                )
                    .setUnbreakable(true)
            ).also {
                it.onClick = context@{ player ->
                    if (player.hasMetadata("frozen"))
                    {
                        player.sendMessage("${CC.RED}You cannot join queues while frozen!")
                        return@context
                    }

                    MinigameModernAccess.playQueueJoinSound(player)
                    JoinQueueMenu(player, QueueType.Casual, 1).openMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            2,
            StaticHotbarPresetEntry(
                hotbarItem(
                    material = MinigameModernAccess.duelsPracticeQueueMaterial(),
                    title = modernName("Practice Queue", "#ffb55c", "#ffe59f", "${CC.GOLD}Play Bot Fights"),
                    lore = arrayOf(
                        "${CC.GRAY}Warm up against bots and test",
                        "${CC.GRAY}new modern duel loadouts.",
                        "",
                        "${CC.GREEN}Right-click to choose a bot fight."
                    )
                )
                    .setUnbreakable(true)
            ).also {
                it.onClick = context@{ player ->
                    if (player.hasMetadata("frozen"))
                    {
                        player.sendMessage("${CC.RED}You cannot join queues while frozen!")
                        return@context
                    }

                    val menu = buildBotSelectorMenu(player)
                        ?: return@context run {
                            player.sendMessage("${CC.RED}This feature is not available on this server!")
                        }

                    MinigameModernAccess.playQueueJoinSound(player)
                    menu.openMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            1,
            StaticHotbarPresetEntry(
                hotbarItem(
                    material = MinigameModernAccess.duelsCompetitiveQueueMaterial(),
                    title = modernName("Competitive Queue", "#5db7ff", "#b9ebff", "${CC.AQUA}Play Ranked"),
                    lore = arrayOf(
                        "${CC.GRAY}Queue into ranked fights and climb",
                        "${CC.GRAY}the competitive ladder.",
                        "",
                        "${CC.GREEN}Right-click to select a ranked queue."
                    )
                )
                    .setUnbreakable(true)
            ).also {
                it.onClick = context@{ player ->
                    MinigameModernAccess.playQueueJoinSound(player)
                    openRankedQueueMenu(player)
                }
            }
        )

        idlePreset.addSlot(
            4,
            createParty
        )

        idlePreset.addSlot(
            6,
            cosmetics
        )

        idlePreset.addSlot(8, settings)

        HotbarPresetHandler.startTrackingHotbar("idle", idlePreset)
        hotbarCache[PlayerState.Idle] = idlePreset

        val inQueuePreset = HotbarPreset()
        inQueuePreset.addSlot(
            8,
            StaticHotbarPresetEntry(
                ItemBuilder(XMaterial.RED_DYE)
                    .name(
                        if (MinigameModernAccess.isModern()) {
                            "${CC.RED}Leave Queue ${CC.D_GRAY}(${CC.WHITE}Cancel Matchmaking${CC.D_GRAY})"
                        } else {
                            "${CC.RED}Leave Queue ${CC.GRAY}(Right Click)"
                        }
                    )
            ).also {
                it.onClick = scope@{ player ->
                    QueueService.leaveQueue(player)
                    MinigameModernAccess.playQueueLeaveSound(player)
                    player.sendMessage(
                        "${CC.RED}You left the queue!"
                    )
                }
            }
        )

        HotbarPresetHandler.startTrackingHotbar("inQueue", inQueuePreset)
        hotbarCache[PlayerState.InQueue] = inQueuePreset

        val leaveParty = StaticHotbarPresetEntry(
            hotbarItem(
                material = Material.matchMaterial("BARRIER") ?: XMaterial.RED_DYE.parseMaterial() ?: Material.INK_SAC,
                title = modernName("Leave Party", "#ff7a7a", "#ffc3c3", "${CC.RED}Leave Party"),
                lore = arrayOf(
                    "${CC.GRAY}Leave your current party and",
                    "${CC.GRAY}return to solo duels.",
                    "",
                    "${CC.GREEN}Right-click to leave."
                )
            )
        ).also {
            it.onClick = { player ->
                player.performCommand("party leave")
                MinigameModernAccess.playQueueLeaveSound(player)
                Tasks.delayed(2L) {
                    LobbyPlayerService.find(player)?.syncQueueState()
                }
            }
        }

        val inPartyLeaderPreset = HotbarPreset()
        inPartyLeaderPreset.addSlot(0, StaticHotbarPresetEntry(
            hotbarItem(
                material = Material.BEACON,
                title = modernName("Party Manager", "#7ce1ff", "#d9faff", "${CC.GOLD}Configure Party"),
                lore = arrayOf(
                    "${CC.GRAY}Manage visibility, invites,",
                    "${CC.GRAY}party settings, and limits.",
                    "",
                    "${CC.GREEN}Right-click to manage your party."
                )
            )
        ).also {
            it.onClick = { player ->
                player.performCommand("party manage")
                MinigameModernAccess.playMenuSound(player)
            }
        })
        inPartyLeaderPreset.addSlot(4, StaticHotbarPresetEntry(
            hotbarItem(
                material = if (MinigameModernAccess.isModern()) Material.matchMaterial("NETHERITE_SPEAR") ?: Material.matchMaterial("SPEAR") ?: Material.matchMaterial("TRIDENT") ?: Material.BEACON else Material.BEACON,
                title = modernName("Party Play", "#78ffb3", "#d8ffd8", "${CC.GREEN}Game Selector"),
                lore = arrayOf(
                    "${CC.GRAY}Start party games and queue",
                    "${CC.GRAY}events with your members.",
                    "",
                    "${CC.GREEN}Right-click to play together."
                )
            )
        ).also {
            it.onClick = { player ->
                PartyPlayGameSelectMenu().openMenu(player)
                MinigameModernAccess.playMenuSound(player)
            }
        })
        inPartyLeaderPreset.addSlot(7, idlePreset.entries[7]!!)
        inPartyLeaderPreset.addSlot(8, leaveParty)

        val inPartyMemberPreset = HotbarPreset()
        inPartyMemberPreset.addSlot(4, StaticHotbarPresetEntry(
            hotbarItem(
                material = Material.matchMaterial("PLAYER_HEAD") ?: Material.PLAYER_HEAD,
                title = modernName("Party Info", "#8ea7ff", "#e1e7ff", "${CC.AQUA}Party Info"),
                lore = arrayOf(
                    "${CC.GRAY}View your current party status",
                    "${CC.GRAY}and available group actions.",
                    "",
                    "${CC.GREEN}Right-click to open party menu."
                )
            )
        ).also {
            it.onClick = { player ->
                PartyPlayGameSelectMenu().openMenu(player)
                MinigameModernAccess.playMenuSound(player)
            }
        })
        inPartyMemberPreset.addSlot(7, idlePreset.entries[7]!!)
        inPartyMemberPreset.addSlot(8, leaveParty)

        HotbarPresetHandler.startTrackingHotbar("inPartyLeader", inPartyLeaderPreset)
        hotbarCache[PlayerState.InPartyAsLeader] = inPartyLeaderPreset

        HotbarPresetHandler.startTrackingHotbar("inPartyMember", inPartyMemberPreset)
        hotbarCache[PlayerState.InPartyAsMember] = inPartyMemberPreset

        if (!SpigotNetworkMetadataDataSync.isFlagged("STRIPPED_LOBBY"))
        {
            Events
                .subscribe(
                    PlayerJoinEvent::class.java,
                    EventPriority.LOW
                )
                .handler { event ->
                    idlePreset.applyToPlayer(event.player)
                }
                .bindWith(plugin)
        }
    }

    fun get(state: PlayerState) = hotbarCache[state]!!
    fun set(state: PlayerState, preset: HotbarPreset)
    {
        hotbarCache[state] = preset
    }

    fun reset(player: Player)
    {
        if (SpigotNetworkMetadataDataSync.isFlagged("STRIPPED_LOBBY"))
        {
            return
        }

        val lobbyPlayer = LobbyPlayerService.find(player.uniqueId)

        if (lobbyPlayer != null)
        {
            val state = lobbyPlayer.state
            val hotbar = get(state)

            if (!Bukkit.isPrimaryThread())
            {
                Tasks.sync {
                    hotbar.applyToPlayer(player)
                    return@sync
                }
            } else
            {
                hotbar.applyToPlayer(player)
            }
        }
    }
}
