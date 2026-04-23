package mc.arch.minigames.versioned.generics

import org.bukkit.entity.Player

interface PlayerProvider
{
    fun obfuscateHealth(player: Player, obfuscated: Boolean)
}
