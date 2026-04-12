package gg.tropic.practice.minigame.handler

import gg.scala.commons.agnostic.sync.ServerSync
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.matchmaking.JoinIntoGameRequest
import gg.tropic.practice.games.matchmaking.JoinIntoGameResult
import gg.tropic.practice.games.matchmaking.JoinIntoGameStatus
import gg.tropic.practice.application.api.defaults.AwareRPCContext
import gg.tropic.practice.application.api.defaults.AwareRPCHandler
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import kotlin.concurrent.withLock
import org.bukkit.Bukkit

class MiniGameJoinIntoGameHandler : AwareRPCHandler<JoinIntoGameRequest, JoinIntoGameResult>
{
    override fun handle(
        request: JoinIntoGameRequest,
        context: AwareRPCContext<JoinIntoGameResult>
    )
    {
        if (ServerSync.local.id != request.server)
        {
            return
        }

        val gameImpl = GameService.gameMappings[request.game.uniqueId]
            ?: return run {
                context.reply(
                    JoinIntoGameResult(
                        uniqueId = request.uniqueId,
                        status = JoinIntoGameStatus.FAILED_GAME_NOT_FOUND
                    )
                )
            }

        var status = JoinIntoGameStatus.SUCCESS
        if (!(gameImpl.state(GameState.Waiting) || gameImpl.state(GameState.Starting)))
        {
            status = JoinIntoGameStatus.FAILED_ALREADY_STARTED
        } else
        {
            gameImpl.teamMutLock.withLock {
                val maxPlayersPerTeam = gameImpl.miniGameLifecycle!!.configuration.maximumPlayersPerTeam
                val partySize = request.players.size

                if (partySize + gameImpl.teams.sumOf { it.players.size } > gameImpl.miniGameLifecycle!!.configuration.maximumPlayers)
                {
                    status = JoinIntoGameStatus.FAILED_NON_EMPTY_TEAMS
                    return@withLock
                }

                request.players.forEach { player ->
                    val team = gameImpl.getNullableTeamOfID(player)
                        ?: return@forEach

                    team.players -= player
                }

                if (partySize > maxPlayersPerTeam)
                {
                    val availableTeams = gameImpl.teams
                        .filter { team ->
                            team.players.size < maxPlayersPerTeam
                        }
                        .sortedBy { it.players.size }

                    val totalAvailableSlots = availableTeams.sumOf { team ->
                        maxPlayersPerTeam - team.players.size
                    }

                    if (totalAvailableSlots < partySize)
                    {

                        status = JoinIntoGameStatus.FAILED_NON_EMPTY_TEAMS
                    } else
                    {

                        val playersToPlace = request.players.toMutableList()

                        for (team in availableTeams)
                        {
                            if (playersToPlace.isEmpty())
                            {
                                break
                            }

                            val availableSpots = maxPlayersPerTeam - team.players.size
                            val playersToAddToThisTeam = minOf(availableSpots, playersToPlace.size)

                            val playersForThisTeam = playersToPlace.take(playersToAddToThisTeam)
                            playersToPlace.removeAll(playersForThisTeam)

                            gameImpl.preWaitAdd(playersForThisTeam, team.teamIdentifier)
                        }

                        if (playersToPlace.isNotEmpty())
                        {

                            request.players.forEach { player ->
                                val team = gameImpl.getTeamOf(player)
                                team?.players?.remove(player)
                            }
                            status = JoinIntoGameStatus.FAILED_NON_EMPTY_TEAMS
                        }
                    }
                } else
                {

                    
                    val emptyTeams = gameImpl.teams.filter { it.players.isEmpty() }
                    if (emptyTeams.isNotEmpty())
                    {
                        val targetTeam = emptyTeams.first()
                        gameImpl.preWaitAdd(request.players.toList(), targetTeam.teamIdentifier)
                    } else
                    {

                        val nonEmptyTeamsWithSpace = gameImpl.teams
                            .filter { it.players.isNotEmpty() && it.players.size + partySize <= maxPlayersPerTeam }
                            .sortedBy { it.players.size } 

                        if (nonEmptyTeamsWithSpace.isNotEmpty())
                        {
                            val targetTeam = nonEmptyTeamsWithSpace.first()
                            gameImpl.preWaitAdd(request.players.toList(), targetTeam.teamIdentifier)
                        } else
                        {

                            val nonEmptyTeams = gameImpl.teams
                                .filter { it.players.isNotEmpty() && it.players.size < maxPlayersPerTeam }
                                .sortedBy { it.players.size }

                            if (nonEmptyTeams.size >= 2)
                            {
                                val firstTeam = nonEmptyTeams[0]
                                val secondTeam = nonEmptyTeams[1]

                                
                                if (secondTeam.players.size + firstTeam.players.size <= maxPlayersPerTeam)
                                {

                                    firstTeam.players.forEach { player ->
                                        secondTeam.players += player

                                        val bukkitPlayer = Bukkit.getPlayer(player)
                                        if (bukkitPlayer != null)
                                        {
                                            NametagHandler.reloadPlayer(bukkitPlayer)
                                            bukkitPlayer.sendMessage("${CC.RED}You have been switched to a different team to accommodate for a party.")
                                        }
                                    }

                                    firstTeam.players.clear()

                                    gameImpl.preWaitAdd(request.players.toList(), firstTeam.teamIdentifier)
                                } else
                                {

                                    status = JoinIntoGameStatus.FAILED_NON_EMPTY_TEAMS
                                }
                            } else
                            {

                                status = JoinIntoGameStatus.FAILED_NON_EMPTY_TEAMS
                            }
                        }
                    }
                }
            }
        }

        if (status == JoinIntoGameStatus.SUCCESS)
        {
            request.players.forEach { player ->
                gameImpl.pendingLogins[player] = System.currentTimeMillis()
            }
        }

        context.reply(JoinIntoGameResult(uniqueId = request.uniqueId, status = status))
    }
}
