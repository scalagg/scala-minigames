package mc.arch.minigames.versioned.generics

import org.bukkit.Location
import org.bukkit.entity.Player

interface PlayerProvider
{
    fun obfuscateHealth(player: Player, obfuscated: Boolean)

    fun mountImmovable(player: Player, location: Location)
    fun unmount(player: Player)

    fun applyResetAttributesLegacyHack(player: Player)
}
