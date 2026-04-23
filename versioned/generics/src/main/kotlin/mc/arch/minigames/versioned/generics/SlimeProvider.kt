package mc.arch.minigames.versioned.generics

interface SlimeProvider
{
    fun queueGenerateWorld(worldGeneric: SlimeWorldGeneric<*>, newName: String)
    fun loadReadOnlyWorld(name: String): SlimeWorldGeneric<*>

    fun loadPersistentHostedWorld(name: String): SlimeWorldGeneric<*>
    fun createEmptyHostedWorld(name: String): SlimeWorldGeneric<*>

    fun saveWorld(generic: SlimeWorldGeneric<*>)
}
