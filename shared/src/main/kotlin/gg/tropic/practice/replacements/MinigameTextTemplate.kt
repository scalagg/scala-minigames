package gg.tropic.practice.replacements

import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.tropic.practice.messaging.ExternalPlayerCountApiSubmissionService
import gg.tropic.practice.metadata.SystemMetadataService
import net.evilblock.cubed.util.math.Numbers

fun String.toTemplatePlayerCounts() = template { key, data ->
    when (key)
    {
        "group" ->
        {
            Numbers.format(
                ServerContainer
                    .getServersInGroupCasted<GameServer>(data)
                    .sumOf { it.getPlayersCount() ?: 0 }
            )
        }

        "legacyplayercount" ->
        {
            Numbers.format(
                ExternalPlayerCountApiSubmissionService.getLegacyPlayerCount(data)
            )
        }

        "minigame" ->
        {
            val (group, minigameID) = data.split("_")
            Numbers.format(
                ServerContainer
                    .getServersInGroupCasted<GameServer>(group)
                    .sumOf { it.getPlayersCount() ?: 0 } +
                    SystemMetadataService.allGames()
                        .filter { (minigameID == "duels" && it.miniGameType == null) || it.miniGameType == minigameID }
                        .sumOf { it.onlinePlayers ?: 0 }
            )
        }

        "hwi" ->
        {
            Numbers.format(
                SystemMetadataService.allHostedWorldInstances()
                    .filter { it.type.name == data }
                    .sumOf { it.onlinePlayers.size }
            )
        }

        // Aggregate duel-queue stats (sum across all kits/team sizes). NPCs that open
        // the kit-select menu use these because they cannot reference a single kit.
        "duelqueue" ->
        {
            Numbers.format(
                when (data)
                {
                    "ranked" -> SystemMetadataService.getTotalRankedQueuePlayers()
                    "unranked", "casual" -> SystemMetadataService.getTotalUnrankedQueuePlayers()
                    else -> 0
                }
            )
        }

        "duelmatches" ->
        {
            Numbers.format(
                when (data)
                {
                    "ranked" -> SystemMetadataService.getActiveRankedMatches()
                    "unranked", "casual" -> SystemMetadataService.getActiveUnrankedMatches()
                    else -> 0
                }
            )
        }

        else -> ""
    }
}
