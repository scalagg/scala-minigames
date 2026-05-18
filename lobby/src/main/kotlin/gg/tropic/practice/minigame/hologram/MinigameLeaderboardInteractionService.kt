package gg.tropic.practice.minigame.hologram

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.minigame.MinigameLobby
import gg.tropic.practice.minigame.hologram.settings.MinigameLeaderboardSettingsMenu
import gg.tropic.practice.minigame.hologram.settings.MinigameLeaderboardSettingsService
import me.lucko.helper.Events
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.entity.EntityHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
object MinigameLeaderboardInteractionService : PacketListenerAbstract()
{
    private const val clickCooldownMs = 225L
    private val clickCooldown = ConcurrentHashMap<UUID, Long>()
    private var registered = false

    @Configure
    @JvmStatic
    fun configure()
    {
        if (!registered) {
            PacketEvents.getAPI().eventManager.registerListener(this)
            registered = true
        }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler { event ->
                clickCooldown.remove(event.player.uniqueId)
            }
    }

    override fun onPacketReceive(event: PacketReceiveEvent)
    {
        if (event.packetType != PacketType.Play.Client.INTERACT_ENTITY) {
            return
        }

        val player = event.getPlayer<Player>() ?: return

        if (!MinigameLobby.isMinigameLobby()) {
            return
        }

        val entityId = WrapperPlayClientInteractEntity(event).entityId

        if (Bukkit.isPrimaryThread()) {
            if (tryOpenSettings(player, entityId)) {
                event.isCancelled = true
            }
            return
        }

        Bukkit.getScheduler().runTask(ScalaCommonsSpigot.instance, Runnable {
            tryOpenSettings(player, entityId)
        })
    }

    private fun tryOpenSettings(player: Player, entityId: Int): Boolean
    {
        val now = System.currentTimeMillis()
        val last = clickCooldown[player.uniqueId] ?: 0L
        if (now - last < clickCooldownMs) {
            return false
        }

        val nearest = EntityHandler
            .getEntitiesByType(MinigameLeaderboardHologramEntity::class.java)
            .asSequence()
            .filter { hologram ->
                hologram.matchesRenderedEntity(player.uniqueId, entityId)
            }
            .minByOrNull { leaderboard ->
                leaderboard.location.distanceSquared(player.location)
            }
            ?: return false

        clickCooldown[player.uniqueId] = now
        MinigameLeaderboardSettingsMenu(
            leaderboard = nearest.leaderboard,
            settings = MinigameLeaderboardSettingsService.load(player.uniqueId, nearest.leaderboard)
        ).openMenu(player)
        return true
    }
}
