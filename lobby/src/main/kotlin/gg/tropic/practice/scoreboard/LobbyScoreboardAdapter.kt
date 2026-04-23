package gg.tropic.practice.scoreboard

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.tablist.TabListService
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.commons.agnostic.sync.ServerSync.getLocalGameServer
import gg.scala.commons.metadata.SpigotNetworkMetadataDataSync
import gg.scala.commons.playerstatus.toPlayerStatus
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.queue.spigot.stream.SpigotRedisService
import gg.scala.game.extensions.economy.EconomyDataSync
import gg.scala.game.extensions.economy.EconomyProfileService
import gg.scala.game.extensions.profile.CorePlayerProfileService
import gg.tropic.practice.minigame.MinigameLobby
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.formattedDomain
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.region.PlayerRegionFromRedisProxy
import gg.tropic.practice.scoreboard.animation.FrameAnimation
import gg.tropic.practice.scoreboard.animation.GradientWaveTitle
import gg.tropic.practice.scoreboard.animation.WaveAnimationTitle
import gg.tropic.practice.scoreboard.configuration.LobbyScoreboardConfigurationService
import gg.tropic.practice.scoreboard.configuration.primaryColor
import gg.tropic.practice.scoreboard.configuration.secondaryColor
import gg.tropic.practice.settings.DuelsSettingCategory
import gg.tropic.practice.settings.isASilentSpectator
import gg.tropic.practice.settings.layout
import gg.tropic.practice.settings.scoreboard.LobbyScoreboardView
import gg.tropic.practice.settings.scoreboard.ScoreboardStyle
import mc.arch.player.status.VisibilityGroup
import me.lucko.helper.Events
import me.lucko.helper.Helper
import net.evilblock.cubed.scoreboard.ScoreboardAdapter
import net.evilblock.cubed.scoreboard.ScoreboardAdapterRegister
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.util.nms.MinecraftProtocol
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.metadata.FixedMetadataValue
import java.text.SimpleDateFormat
import java.util.*

@Service
@ScoreboardAdapterRegister
object LobbyScoreboardAdapter : ScoreboardAdapter()
{
    val dateFormat = SimpleDateFormat("MM/dd/yy")

    override fun getLines(board: LinkedList<String>, player: Player)
    {
        val layout: ScoreboardStyle = layout(player)
        val profile = LobbyPlayerService.find(player.uniqueId)
            ?: return
        val coreProfile = CorePlayerProfileService.find(player)

        if (layout == ScoreboardStyle.Disabled)
        {
            return
        }

        board += "${CC.GRAY}${dateFormat.format(Date())} ${CC.D_GRAY}${
            getLocalGameServer()
                .id
                .split("-")
                .lastOrNull() ?: "??"
        }"

        board += ""

        if (!(MinigameLobby.isMinigameLobby() || MinigameLobby.isMainLobby()))
        {
            board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Online: ${CC.GOLD}${
                Numbers.format(ScoreboardInfoService.scoreboardInfo.online)
            }"
            board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}In-game: ${CC.GOLD}${
                Numbers.format(ScoreboardInfoService.scoreboardInfo.playing)
            }"
        } else
        {
            if (MinigameLobby.isMainLobby())
            {
                board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Rank: ${
                    QuickAccess.realRank(player).getColoredName()
                }"
                if (coreProfile != null)
                {
                    board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Level: ${CC.AQUA}${
                        Numbers.format(coreProfile.level)
                    }"
                }

                board += ""

                board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Lobby: ${CC.AQUA}#${
                    MinigameLobby.lobbies[ServerSync.local.id]?.friendlyId ?: "1"
                }"
                board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Players: ${CC.AQUA}${
                    Numbers.format(MinigameLobby.globalPlayerCount)
                }"

            } else
            {
                if (SpigotNetworkMetadataDataSync.isFlagged("BOARD_OVERRIDE_LOBBY"))
                {
                    board.clear()
                    board += MinigameLobby.customizer()
                        .scoreboard()
                        .provideIdleLines(player, profile)
                    return
                } else
                {
                    board.clear()
                    board += MinigameLobby.customizer()
                        .scoreboard()
                        .provideIdleLines(player, profile)
                    return
                }
            }

            if (profile.isNetworkQueue())
            {
                val queue = SpigotRedisService.findQueuePlayerIsIn(player)
                if (queue != null)
                {
                    board += ""
                    board += "${primaryColor()}${queue.displayName} Queue:"
                    board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${CC.GRAY}Position: ${CC.WHITE}${
                        Numbers.format(
                            queue.position(player.uniqueId)
                        )
                    }${CC.GRAY}/${CC.WHITE}${
                        Numbers.format(
                            queue.players.size
                        )
                    }"
                }
            } else if (profile.inQueue())
            {
                board += ""
                board += "${primaryColor()}${profile.queuedForType().name} Queue:"
                board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}${profile.queuedForKit()?.displayName} ${
                    profile.queuedForTeamSize()
                }v${
                    profile.queuedForTeamSize()
                }"
                board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Queued for ${CC.WHITE}${
                    TimeUtil.formatIntoMMSS((profile.queuedForTime() / 1000).toInt())
                }"

                val shouldIncludeELORange = profile.validateQueueEntry() &&
                    profile.queuedForType() == QueueType.Ranked &&
                    MinecraftProtocol.getPlayerVersion(player) <= 5

                if (shouldIncludeELORange)
                {
                    val domain = profile.queueEntry().leaderRangedELO
                        .toIntRangeInclusive()
                        .formattedDomain()

                    board += "${CC.GRAY}ELO Range: ${primaryColor()}$domain"
                }
            } else if (profile.isInParty())
            {
                board += " "
                board += "${primaryColor()}Party:"

                with(profile.partyOf().delegate) {
                    board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Leader: ${CC.WHITE}${
                        leader.uniqueId.username()
                    }"
                    board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Online: ${CC.WHITE}${profile.partyOf().currentPlayers}${CC.GRAY}/${CC.WHITE}$limit"
                    board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Party is " + status.capitalized + "..."
                }
            } else
            {
                if (!(MinigameLobby.isMinigameLobby() || MinigameLobby.isMainLobby()))
                {
                    if (coreProfile != null)
                    {
                        board += ""
                        board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Level: ${CC.AQUA}${
                            Numbers.format(coreProfile.level)
                        } ${Constants.EXP_SYMBOL}"

                        val economyProfile = EconomyProfileService.find(player)
                        if (economyProfile != null)
                        {
                            board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Coins: ${CC.GOLD}${
                                Numbers.format(economyProfile.balance("duels"))
                            } ${Constants.TOKENS_SYMBOL}"
                        }
                    }
                }
            }

            BasicsProfileService.find(player)
                ?.apply {
                    val scoreboardView = setting(
                        "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:lobby-scoreboard-view",
                        LobbyScoreboardView.None
                    )

                    if (scoreboardView != LobbyScoreboardView.None)
                    {
                        board += ""
                        board += "${CC.AQUA}${scoreboardView.displayName}:"
                    }

                    when (scoreboardView)
                    {
                        LobbyScoreboardView.Dev ->
                        {
                            board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Game servers: ${CC.AQUA}${
                                ScoreboardInfoService.scoreboardInfo.gameServers
                            }"
                            board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Mean TPS: ${CC.AQUA}${
                                ScoreboardInfoService.scoreboardInfo.meanTPS.run {
                                    if (this > 20.0) "+20.0" else "%.1f".format(ScoreboardInfoService.scoreboardInfo.meanTPS)
                                }
                            }"
                            board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Games: ${CC.AQUA}${
                                Numbers.format(ScoreboardInfoService.scoreboardInfo.runningGames)
                            } ${CC.D_GRAY}(${
                                "%.2f".format(ScoreboardInfoService.scoreboardInfo.percentagePlaying)
                            }%)"
                        }

                        LobbyScoreboardView.Staff ->
                        {
                            fun metadataDisplay(metadata: String) = if (player.hasMetadata(metadata))
                                "${CC.GREEN}Enabled" else "${CC.RED}Disabled"

                            val playerStatus = player.toPlayerStatus()
                            val basicsProfile = BasicsProfileService.find(player)

                            board.add(
                                "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${CC.GRAY}Group: ${
                                    when (playerStatus.visibilityGroup())
                                    {
                                        VisibilityGroup.ALL -> "${CC.GREEN}All"
                                        VisibilityGroup.STAFF -> "${CC.BLUE}Staff"
                                        VisibilityGroup.ADMIN -> "${CC.RED}Admin"
                                        VisibilityGroup.OPERATOR -> "${CC.D_RED}Owner"
                                        else -> "${CC.GRAY}Custom"
                                    }
                                }"
                            )

                            if (basicsProfile != null)
                            {
                                board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Silent Spec: ${
                                    if (player.isASilentSpectator()) "${CC.GREEN}Enabled" else "${CC.RED}Disabled"
                                }"
                            }

                            board += "${primaryColor()}${Constants.THIN_VERTICAL_LINE} ${secondaryColor()}Mod Mode: ${
                                metadataDisplay(
                                    "mod-mode"
                                )
                            }"
                        }

                        LobbyScoreboardView.None ->
                        {

                        }
                    }
                }
        }

        board += ""
        board += "${CC.GRAY}${LemonConstants.WEB_LINK}${CC.GRAY}      ${primaryColor()}"
    }

    @Configure
    fun configure()
    {
        Tasks.asyncTimer(FrameAnimation, 1L, 1L)

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                PlayerRegionFromRedisProxy.of(it.player)
                    .thenAccept { region ->
                        it.player.setMetadata(
                            "region",
                            FixedMetadataValue(Helper.hostPlugin(), region.name)
                        )
                    }
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                it.player.removeMetadata("region", Helper.hostPlugin())
            }
    }

    override fun getTitle(player: Player): String
    {
        if (MinigameLobby.isMainLobby())
        {
            if (LobbyScoreboardConfigurationService.cached().titleAnimated)
            {
                return WaveAnimationTitle.getCurrentTitle()
            }

            return LobbyScoreboardConfigurationService.cached().title
        }

        if (MinigameLobby.isMinigameLobby())
        {
            return MinigameLobby.customizer().scoreboard().provideTitle()
        }

        return GradientWaveTitle.render("DUELS", "#F5C037", "#FC695C") +
            if ("mipdev" in ServerSync.getLocalGameServer().groups) " ${CC.GRAY}(dev)" else ""
    }
}
