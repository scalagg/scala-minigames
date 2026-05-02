package mc.arch.minigames.versioned.modern

import mc.arch.minigames.versioned.generics.PlayerProvider
import org.bukkit.Location
import org.bukkit.entity.Bat
import org.bukkit.entity.Player

object ModernPlayerProvider : PlayerProvider
{
    private const val MOUNT_TAG = "scala_immovable_mount"

    override fun obfuscateHealth(player: Player, obfuscated: Boolean)
    {

    }

    override fun mountImmovable(player: Player, location: Location)
    {
        val world = location.world ?: return
        val bat: Bat = world.spawn(
            location.clone().add(0.0, 0.5, 0.0),
            Bat::class.java
        )
        bat.isInvisible = true
        bat.setAI(false)
        bat.isSilent = true
        bat.addScoreboardTag(MOUNT_TAG)
        bat.addPassenger(player)
    }

    override fun unmount(player: Player)
    {
        val vehicle = player.vehicle as? Bat ?: return
        if (!vehicle.scoreboardTags.contains(MOUNT_TAG)) return
        vehicle.removePassenger(player)
        vehicle.remove()
    }

    override fun applyResetAttributesLegacyHack(player: Player)
    {

    }
}
