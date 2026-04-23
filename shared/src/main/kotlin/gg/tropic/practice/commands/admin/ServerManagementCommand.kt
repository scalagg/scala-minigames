package gg.tropic.practice.commands.admin

import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.tropic.practice.persistence.RedisShared
import net.evilblock.cubed.util.CC
import org.bukkit.command.CommandSender
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * @author Limo
 * @date 2/15/2026
 */
@AutoRegister
@CommandAlias("servermanager")
@CommandPermission("commons.command.serversync")
object ServerManagementCommand : ScalaCommand()
{
    private val pendingResponses = ConcurrentHashMap<String, CompletableFuture<String>>()

    init
    {
        RedisShared.applicationBridge.configure {
            listen("management-response") {
                val requestId = retrieve<String>("requestId")
                val result = retrieve<String>("result")

                pendingResponses.remove(requestId)?.complete(result)
            }
        }
    }

    private fun sendManagementCommand(
        sender: CommandSender,
        packet: String,
        vararg pairs: Pair<String, Any?>
    )
    {
        val requestId = UUID.randomUUID().toString()
        val future = CompletableFuture<String>()
        pendingResponses[requestId] = future

        RedisShared.applicationBridge.createMessage(
            packet,
            "requestId" to requestId,
            *pairs
        ).publish(AwareThreadContext.SYNC)

        sender.sendMessage("${CC.GRAY}Sending request...")

        CompletableFuture.runAsync {
            try
            {
                val result = future.get(10, TimeUnit.SECONDS)
                result.split("\n").forEach { line ->
                    sender.sendMessage("${CC.GREEN}$line")
                }
            }
            catch (e: Exception)
            {
                pendingResponses.remove(requestId)
                sender.sendMessage("${CC.RED}Request timed out or failed: ${e.message}")
            }
        }
    }

    private fun allServerIds(): List<String>
    {
        return ServerContainer
            .allServers()
            .values
            .map { it.id }
            .sorted()
    }

    @Default
    @HelpCommand
    fun onHelp(sender: CommandSender)
    {
        sender.sendMessage("${CC.AQUA}${CC.BOLD}Server Manager ${CC.GRAY}- Commands")
        sender.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}                                        ")
        sender.sendMessage(" ${CC.YELLOW}/servermanager list ${CC.GRAY}- List all servers")
        sender.sendMessage(" ${CC.YELLOW}/servermanager info <server> ${CC.GRAY}- View server details")
        sender.sendMessage(" ${CC.YELLOW}/servermanager drain <server> ${CC.GRAY}- Mark server for draining")
        sender.sendMessage(" ${CC.YELLOW}/servermanager stop <server> ${CC.GRAY}- Force stop a server pod")
        sender.sendMessage(" ${CC.YELLOW}/servermanager restart <type> ${CC.GRAY}- Rolling restart a deployment")
        sender.sendMessage(" ${CC.YELLOW}/servermanager scale <type> <count> ${CC.GRAY}- Scale servers")
        sender.sendMessage(" ${CC.YELLOW}/servermanager deployments ${CC.GRAY}- List K8s deployments")
        sender.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}                                        ")
        sender.sendMessage(" ${CC.GRAY}Deployment types: ${CC.WHITE}proxy, mainlobby, bwlobby, application")
        sender.sendMessage(" ${CC.GRAY}Game types: ${CC.WHITE}bedwars")
    }

    @Subcommand("list")
    @Description("List all servers with status information")
    fun onList(sender: CommandSender)
    {
        val servers = ServerContainer
            .allServers()
            .values
            .sortedBy { it.id }

        if (servers.isEmpty())
        {
            sender.sendMessage("${CC.RED}No servers found.")
            return
        }

        sender.sendMessage("${CC.AQUA}${CC.BOLD}Server List ${CC.GRAY}(${servers.size} servers)")
        sender.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}                                        ")

        val drainFlags = try
        {
            RedisShared.keyValueCache.sync().hgetall("operator:drain-flags")
        }
        catch (e: Exception) { emptyMap() }

        for (abstractServer in servers)
        {
            val draining = drainFlags.containsKey(abstractServer.id)
            val groups = abstractServer.groups.joinToString(", ")
            val gameServer = abstractServer as? GameServer

            val players = gameServer?.getPlayersCount() ?: 0
            val maxPlayers = gameServer?.getMaxPlayers() ?: 0
            val tps = gameServer?.getTPS()?.let { String.format("%.1f", it) } ?: "?"

            val stateColor = when
            {
                draining -> "${CC.RED}[DRAINING]"
                else -> "${CC.GREEN}[${abstractServer.state.name.uppercase()}]"
            }

            sender.sendMessage(
                " ${CC.GRAY}- ${CC.WHITE}${abstractServer.id} $stateColor " +
                "${CC.GRAY}| ${CC.YELLOW}$players/$maxPlayers ${CC.GRAY}| " +
                "TPS: ${CC.WHITE}$tps ${CC.GRAY}| ${CC.DARK_GRAY}$groups"
            )
        }

        sender.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}                                        ")
    }

    @Subcommand("info")
    @Description("View detailed information about a server")
    @CommandCompletion("@sm-servers")
    fun onInfo(sender: CommandSender, serverId: String)
    {
        val abstractServer = ServerContainer.getServer(serverId)

        if (abstractServer == null)
        {
            sender.sendMessage("${CC.RED}Server '$serverId' not found.")
            return
        }

        val groups = abstractServer.groups.joinToString(", ")
        val uptime = formatUptime(System.currentTimeMillis() - abstractServer.initialHeartbeat)

        val draining = try
        {
            RedisShared.keyValueCache.sync().hexists("operator:drain-flags", serverId)
        }
        catch (e: Exception) { false }

        sender.sendMessage("${CC.AQUA}${CC.BOLD}Server Info: ${CC.WHITE}$serverId")
        sender.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}                                        ")
        sender.sendMessage(" ${CC.GRAY}State: ${if (draining) "${CC.RED}DRAINING" else "${CC.GREEN}${abstractServer.state.name.uppercase()}"}")
        sender.sendMessage(" ${CC.GRAY}Uptime: ${CC.WHITE}$uptime")
        sender.sendMessage(" ${CC.GRAY}Groups: ${CC.WHITE}$groups")

        val gameServer = abstractServer as? GameServer
        if (gameServer != null)
        {
            val players = gameServer.getPlayersCount() ?: 0
            val maxPlayers = gameServer.getMaxPlayers() ?: 0
            val tps = gameServer.getTPS()?.let { String.format("%.1f", it) } ?: "?"
            val cpu = gameServer.getCpuUsage()?.let { String.format("%.1f%%", it) } ?: "?"
            val host = gameServer.getHost() ?: "?"
            val port = gameServer.getPort() ?: "?"
            val software = gameServer.getSoftware() ?: "?"
            val whitelisted = gameServer.getWhitelisted() ?: false

            sender.sendMessage(" ${CC.GRAY}Players: ${CC.WHITE}$players/$maxPlayers")
            sender.sendMessage(" ${CC.GRAY}TPS: ${CC.WHITE}$tps")
            sender.sendMessage(" ${CC.GRAY}CPU: ${CC.WHITE}$cpu")
            sender.sendMessage(" ${CC.GRAY}Host: ${CC.WHITE}$host:$port")
            sender.sendMessage(" ${CC.GRAY}Software: ${CC.WHITE}$software")
            sender.sendMessage(" ${CC.GRAY}Whitelist: ${if (whitelisted) "${CC.GREEN}ON" else "${CC.RED}OFF"}")

            try
            {
                val onlinePlayers = gameServer.getPlayers()
                if (onlinePlayers != null && onlinePlayers.isNotEmpty())
                {
                    val playerNames = onlinePlayers.map { it.toString().substring(0, 8) }
                    sender.sendMessage(" ${CC.GRAY}Online: ${CC.WHITE}${playerNames.joinToString(", ")}")
                }
            }
            catch (ignored: Exception)
            {

            }
        }

        sender.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}                                        ")
    }

    @Subcommand("drain")
    @Description("Mark a game server for draining")
    @CommandCompletion("@sm-servers")
    fun onDrain(sender: CommandSender, serverId: String)
    {
        val server = ServerContainer.getServer(serverId)
        if (server == null)
        {
            sender.sendMessage("${CC.RED}Server '$serverId' not found.")
            return
        }

        val alreadyDraining = try
        {
            RedisShared.keyValueCache.sync().hexists("operator:drain-flags", serverId)
        }
        catch (e: Exception) { false }

        if (alreadyDraining)
        {
            sender.sendMessage("${CC.RED}Server '$serverId' is already marked for draining.")
            return
        }

        sendManagementCommand(sender, "server-drain", "serverId" to serverId)
    }

    @Subcommand("stop")
    @Description("Stop and delete a game server pod")
    @CommandCompletion("@sm-servers")
    fun onStop(sender: CommandSender, serverId: String)
    {
        val server = ServerContainer.getServer(serverId)
        if (server == null)
        {
            sender.sendMessage("${CC.RED}Server '$serverId' not found.")
            return
        }

        sendManagementCommand(sender, "server-stop", "serverId" to serverId)
    }

    @Subcommand("restart")
    @Description("Rolling restart a deployment (proxy, mainlobby, bwlobby, application)")
    @CommandCompletion("proxy|mainlobby|bwlobby|application")
    fun onRestart(sender: CommandSender, type: String)
    {
        sendManagementCommand(sender, "server-restart", "type" to type)
    }

    @Subcommand("scale")
    @Description("Scale a deployment or game server type to a specific count")
    @CommandCompletion("proxy|mainlobby|bwlobby|application|bedwars @range:1-10")
    fun onScale(sender: CommandSender, type: String, amount: Int)
    {
        if (amount < 0 || amount > 10)
        {
            sender.sendMessage("${CC.RED}Amount must be between 0 and 10.")
            return
        }

        sendManagementCommand(sender, "server-scale", "type" to type, "amount" to amount)
    }

    @Subcommand("deployments")
    @Description("List all Kubernetes deployments and their status")
    fun onDeployments(sender: CommandSender)
    {
        sendManagementCommand(sender, "server-deployments")
    }

    private fun formatUptime(millis: Long): String
    {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when
        {
            days > 0 -> "${days}d ${hours % 24}h ${minutes % 60}m"
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
