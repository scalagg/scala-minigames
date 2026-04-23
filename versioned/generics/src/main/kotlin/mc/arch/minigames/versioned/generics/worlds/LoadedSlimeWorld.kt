package mc.arch.minigames.versioned.generics.worlds

import mc.arch.minigames.versioned.generics.SlimeWorldGeneric
import org.bukkit.World

data class LoadedSlimeWorld(
    val bukkitWorld: World,
    val generic: SlimeWorldGeneric<*>
)
