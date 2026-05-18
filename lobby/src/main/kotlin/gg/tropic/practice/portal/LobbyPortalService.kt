package gg.tropic.practice.portal

import com.cryptomorin.xseries.XSound
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeLobby
import gg.tropic.practice.player.LobbyPlayerService
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.bukkit.EventUtils
import org.bukkit.Material
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@Service
object LobbyPortalService : DataSyncService<LobbyPortalContainer>()
{
    private val waterMaterial by lazy {
        Material.matchMaterial("WATER")
    }

    private val stationaryWaterMaterial by lazy {
        Material.matchMaterial("STATIONARY_WATER")
    }

    @Inject
    lateinit var plugin: PracticeLobby

    override fun keys() = LobbyPortalKeys
    override fun type() = LobbyPortalContainer::class.java

    override fun locatedIn() = DataSyncSource.Mongo

    @Configure
    fun configure()
    {
        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter { EventUtils.hasPlayerMoved(it) }
            .handler { event ->
                val blockToTest = event.to.block.location.toVector()
                val player = event.player

                val blockType = event.to.block.type
                if (blockType != waterMaterial && blockType != stationaryWaterMaterial)
                {
                    return@handler
                }

                val portal = cached().portals.values
                    .firstOrNull {
                        ServerSync.getLocalGameServer().groups.first() == it.server &&
                            it.blocks.any { block -> block.toVector().equals(blockToTest) }
                    }
                    ?: return@handler

                val profile = LobbyPlayerService.find(player)
                    ?: return@handler

                if (!profile.isNetworkQueue())
                {
                    val lastPortalEnter = player.getMetadata("last-portal-enter")
                        .firstOrNull()
                        ?.asLong()
                        ?: 0

                    if (System.currentTimeMillis() - lastPortalEnter < 5000L)
                    {
                        return@handler
                    }

                    player.setMetadata(
                        "last-portal-enter",
                        FixedMetadataValue(plugin, System.currentTimeMillis())
                    )

                    player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.NAUSEA,
                            5 * 20,
                            1
                        )
                    )

                    XSound.BLOCK_PORTAL_TRAVEL.play(player, 0.5f, 1.0f)

                    Schedulers
                        .sync()
                        .runLater({
                            player.performCommand(
                                "joinq ${portal.destination}"
                            )
                        }, 40L)
                }
            }
    }
}
